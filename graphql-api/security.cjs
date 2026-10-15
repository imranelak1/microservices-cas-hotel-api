const jwt=require('jsonwebtoken');
const secret=process.env.JWT_SECRET||'course7-hotel-local-secret-32-characters-minimum';
function issueToken(input){
  if(input.username!=='student'||input.password!=='labpass')throw new Error('Identifiants invalides');
  return jwt.sign({sub:'student'},secret,{issuer:'hotel-lab',expiresIn:'1h'});
}
function authenticate(authorization) {
  if(!authorization?.startsWith('Bearer ')) throw new Error('JWT requis');
  return jwt.verify(authorization.slice(7),secret,{issuer:'hotel-lab',algorithms:['HS256']});
}
function validate(input) {
  if(!input || !Number.isSafeInteger(Number(input.clientId)) || Number(input.clientId)<1 || !Number.isSafeInteger(Number(input.chambreId)) || Number(input.chambreId)<1) throw new Error('Client et chambre requis');
  const pattern=/^\d{4}-\d{2}-\d{2}$/;
  if(!pattern.test(input.dateDebut)||!pattern.test(input.dateFin)||!(new Date(input.dateFin)>new Date(input.dateDebut))) throw new Error('Le départ doit suivre l’arrivée');
  if((input.preferences||'').length>102400) throw new Error('Préférences limitées à 100 Ko');
}
module.exports={authenticate,validate,issueToken};
