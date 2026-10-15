import http from 'k6/http';
import grpc from 'k6/net/grpc';
import {check} from 'k6';

const host=__ENV.HOST||'localhost';
const protocol=__ENV.PROTOCOL||'rest';
const size=Number(__ENV.SIZE_KIB||1);
const client=new grpc.Client();
client.load(['../grpc-api/src/main/proto'],'hotel.proto');
export const options={stages:__ENV.SMOKE?[{duration:'2s',target:2}]:[
  {duration:'10s',target:10},{duration:'20s',target:10},
  {duration:'10s',target:100},{duration:'20s',target:100},
  {duration:'15s',target:500},{duration:'20s',target:500},
  {duration:'15s',target:1000},{duration:'20s',target:1000},{duration:'5s',target:0}],
  thresholds:{checks:['rate>0.95']}};
export function setup(){
  const response=http.post(`http://${host}:19601/auth/token`,JSON.stringify({username:'student',password:'labpass'}),{headers:{'Content-Type':'application/json'}});
  check(response,{'login JWT':r=>r.status===200});return{token:response.json('token')};
}
function call(operation,id,input,token){
  const headers={Authorization:`Bearer ${token}`,'Content-Type':'application/json'};
  if(protocol==='grpc'){
    const names={create:'CreateReservation',read:'GetReservation',update:'UpdateReservation',delete:'DeleteReservation'};
    const body=operation==='create'?input:operation==='update'?{id:String(id),input}:{id:String(id)};
    const response=client.invoke(`hotel.HotelService/${names[operation]}`,body,{metadata:{authorization:`Bearer ${token}`},timeout:'15s'});
    return{ok:response.status===grpc.StatusOK,id:response.message&&response.message.id};
  }
  if(protocol==='graphql'){
    const queries={create:'mutation($input:ReservationInput!){createReservation(input:$input){id}}',read:'query($id:ID!){reservation(id:$id){id preferences}}',update:'mutation($id:ID!,$input:ReservationInput!){updateReservation(id:$id,input:$input){id}}',delete:'mutation($id:ID!){deleteReservation(id:$id)}'};
    const variables=operation==='create'?{input}:operation==='update'?{id:String(id),input}:{id:String(id)};
    const response=http.post(`http://${host}:19603/graphql`,JSON.stringify({query:queries[operation],variables}),{headers});
    const json=response.json();return{ok:response.status===200&&!json.errors,id:json.data&&json.data.createReservation&&json.data.createReservation.id};
  }
  if(protocol==='soap'){
    const names={create:'createReservation',read:'getReservation',update:'updateReservation',delete:'deleteReservation'};
    const fields=Object.keys(input).map(key=>`<${key}>${input[key]}</${key}>`).join('');
    const body=`<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:h="http://hotel.imrane.ma/"><s:Body><h:${names[operation]}>${operation==='create'?'':`<id>${id}</id>`}${operation==='create'||operation==='update'?`<input>${fields}</input>`:''}</h:${names[operation]}></s:Body></s:Envelope>`;
    const response=http.post(`http://${host}:19602/ws/hotel`,body,{headers:Object.assign({},headers,{'Content-Type':'text/xml;charset=UTF-8'})});
    const found=response.body.match(/<id>(\d+)<\/id>/);return{ok:response.status===200&&!response.body.includes(':Fault'),id:found&&found[1]};
  }
  const methods={create:'POST',read:'GET',update:'PUT',delete:'DELETE'};
  const response=http.request(methods[operation],`http://${host}:19601/api/reservations${operation==='create'?'':`/${id}`}`,
    operation==='create'||operation==='update'?JSON.stringify(input):null,{headers});
  return{ok:response.status>=200&&response.status<300,id:operation==='create'&&response.status===201?response.json('id'):id};
}
export default function(data){
  if(protocol==='grpc')client.connect(`${host}:19605`,{plaintext:true});
  const input={clientId:1,chambreId:__VU,dateDebut:'2090-01-01',dateFin:'2090-01-03',preferences:''};
  input.preferences='x'.repeat(Math.max(0,size*1024-JSON.stringify(input).length));
  const created=call('create',0,input,data.token);
  check(created,{'create':r=>r.ok});
  if(created.ok){
    check(call('read',created.id,input,data.token),{'read':r=>r.ok});
    input.dateFin='2090-01-04';check(call('update',created.id,input,data.token),{'update':r=>r.ok});
    check(call('delete',created.id,input,data.token),{'delete':r=>r.ok});
  }
  if(protocol==='grpc')client.close();
}
