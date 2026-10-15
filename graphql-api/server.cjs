require('./tracing.cjs');
const {ApolloServer}=require('@apollo/server');
const {expressMiddleware}=require('@apollo/server/express4');
const {GraphQLError}=require('graphql');
const express=require('express');const cors=require('cors');const fs=require('node:fs');const path=require('node:path');
const prom=require('prom-client');const repository=require('./repository.cjs');const {authenticate}=require('./security.cjs');
const {log}=require('./logging.cjs');
prom.collectDefaultMetrics();
const histogram=new prom.Histogram({name:'hotel_graphql_request_seconds',help:'Durée GraphQL réelle',labelNames:['operation','status'],buckets:[.001,.005,.01,.025,.05,.1,.25,.5,1,5]});
const resolvers={Query:{reservations:()=>repository.list(),reservation:(_,args)=>repository.find(args.id),clients:()=>repository.clients(),chambres:()=>repository.chambres()},Mutation:{createReservation:(_,args)=>repository.create(args.input),updateReservation:(_,args)=>repository.update(args.id,args.input),deleteReservation:(_,args)=>repository.delete(args.id)}};
async function main() {
  const server=new ApolloServer({typeDefs:fs.readFileSync(path.join(__dirname,'schema.graphql'),'utf8'),resolvers,plugins:[{async requestDidStart(){const start=process.hrtime.bigint();return{async willSendResponse(context){const seconds=Number(process.hrtime.bigint()-start)/1e9;histogram.labels(context.operationName||'anonymous',context.errors?'error':'ok').observe(seconds);log({duration:seconds,error:Boolean(context.errors)});}};}}]});
  await server.start();const app=express();
  app.get('/health',async(req,res)=>{try{await repository.pool.query('SELECT 1');res.json({status:'UP'});}catch(error){res.status(503).json({status:'DOWN'});}});
  app.get('/metrics',async(req,res)=>res.type(prom.register.contentType).send(await prom.register.metrics()));
  app.use('/graphql',cors({origin:'http://localhost:13082'}),express.json({limit:'2mb'}),expressMiddleware(server,{context:async({req})=>{try{return{user:authenticate(req.headers.authorization)};}catch(error){throw new GraphQLError('JWT requis',{extensions:{code:'UNAUTHENTICATED',http:{status:401}}});}}}));
  const port=Number(process.env.PORT||19603);
  if(process.env.TLS_KEY && process.env.TLS_CERT){require('node:https').createServer({key:fs.readFileSync(process.env.TLS_KEY),cert:fs.readFileSync(process.env.TLS_CERT)},app).listen(port,'0.0.0.0',()=>console.log('GraphQL HTTPS '+port));}
  else app.listen(port,'0.0.0.0',()=>console.log('GraphQL http://localhost:'+port+'/graphql'));
}
main().catch(error=>{console.error(error);process.exit(1);});
