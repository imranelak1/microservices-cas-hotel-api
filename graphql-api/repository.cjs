const {Pool}=require('pg');
const fs=require('node:fs');const path=require('node:path');
const sql=JSON.parse(fs.readFileSync(path.join(__dirname,'../database/queries.json'),'utf8'));
const pool=new Pool({connectionString:process.env.DATABASE_URL||'postgresql://lab:labpass@localhost:19632/hotel',max:20});
const {validate}=require('./security.cjs');
async function find(id,db=pool) {const result=await db.query(sql.find,[id]);if(!result.rows.length)throw new Error('Réservation introuvable');return result.rows[0];}
async function change(name,input,id) {
  validate(input);const db=await pool.connect();
  try {
    await db.query('BEGIN');
    const values=[input.clientId,input.chambreId,input.dateDebut,input.dateFin,input.preferences||''];
    if(name==='update')values.push(id);
    const result=await db.query(sql[name],values);if(!result.rows.length)throw new Error('Réservation ou chambre introuvable');
    const reservation=await find(result.rows[0].id,db);await db.query('COMMIT');return reservation;
  }catch(error){await db.query('ROLLBACK');throw error;}finally{db.release();}
}
module.exports={pool,find,create:input=>change('create',input),update:(id,input)=>change('update',input,id),
  list:async()=>{const ids=await pool.query(sql.list);return Promise.all(ids.rows.map(row=>find(row.id)));},
  clients:async()=>(await pool.query(sql.clients)).rows,chambres:async()=>(await pool.query(sql.chambres)).rows,
  delete:async id=>(await pool.query(sql.delete,[id])).rows.length>0};
