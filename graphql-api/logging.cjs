const net=require('node:net');
let socket;
function log(event){
  if(process.env.BENCHMARK==='true')return;
  const message=JSON.stringify({service:'hotel-graphql',time:new Date().toISOString(),...event});
  console.log(message);
  if(!process.env.LOGSTASH_HOST)return;
  if(!socket || socket.destroyed){
    socket=net.createConnection({host:process.env.LOGSTASH_HOST,port:Number(process.env.LOGSTASH_PORT||19644)});
    socket.on('error',()=>{socket.destroy();});
  }
  socket.write(message+'\n');
}
module.exports={log};
