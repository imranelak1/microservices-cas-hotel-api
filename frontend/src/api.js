const base=import.meta.env.VITE_API_URL || 'http://localhost:19601';
export async function request(path,{token,method='GET',body}={}) {
 const response=await fetch(base+path,{method,headers:{Accept:'application/json',...(body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});
 const text=await response.text();let data;try{data=text?JSON.parse(text):null;}catch{data={message:text};}
 if(!response.ok){const error=new Error(data?.message||data?.error||'La requête a échoué ('+response.status+').');error.status=response.status;throw error;}
 return data;
}
