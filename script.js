const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s);
let playing=false,speed=0,tripKm=0,tripSeconds=0,navActive=false,heading=0,deferredInstallPrompt=null;

function clock(){const d=new Date();$("#clock").textContent=d.toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"})}
setInterval(clock,1000);clock();

const installBtn=$("#installBtn");
function isStandalone(){return window.matchMedia("(display-mode: standalone)").matches||window.navigator.standalone===true}
function updateInstallUI(){
  if(!installBtn)return;
  if(isStandalone()){
    installBtn.hidden=true;
    $("#systemState").textContent="INSTALLED";
    $("#systemDetail").textContent="Home-screen app ready";
  }else{
    installBtn.hidden=false;
    installBtn.textContent=deferredInstallPrompt?"Install":"Add to Home";
  }
}
window.addEventListener("beforeinstallprompt",e=>{e.preventDefault();deferredInstallPrompt=e;updateInstallUI()});
installBtn.onclick=async()=>{
  if(deferredInstallPrompt){deferredInstallPrompt.prompt();try{await deferredInstallPrompt.userChoice}catch(e){}deferredInstallPrompt=null;updateInstallUI();return}
  show("Install AutoDash","<p><b>Android Chrome:</b> tap <b>⋮</b> → <b>Add to Home screen</b> or <b>Install app</b>.</p><p>Then open AutoDash from its home-screen icon.</p>");
};
window.addEventListener("appinstalled",()=>{deferredInstallPrompt=null;updateInstallUI()});
updateInstallUI();

if("serviceWorker" in navigator){window.addEventListener("load",async()=>{try{const registration=await navigator.serviceWorker.register("./sw.js",{scope:"./"});await registration.update();$("#systemState").textContent=isStandalone()?"INSTALLED":"READY";$("#systemDetail").textContent="PWA + offline cache"}catch(e){$("#systemState").textContent="ONLINE";$("#systemDetail").textContent="Browser mode"}})}

function updateSpeed(){$("#speed").textContent=Math.round(speed);$("#driveState").textContent=speed>2?"DRIVING":"PARKED"}
setInterval(()=>{speed=navActive?Math.min(62,speed+Math.random()*5):Math.max(0,speed-Math.random()*4);updateSpeed()},1200);
setInterval(()=>{if(navActive){tripSeconds++;if(speed>1)tripKm+=speed/3600}$("#tripTime").textContent=`${String(Math.floor(tripSeconds/60)).padStart(2,"0")}:${String(tripSeconds%60).padStart(2,"0")}`;$("#trip").textContent=tripKm.toFixed(1)+" km"},1000);

$("#themeBtn").onclick=()=>{document.body.classList.toggle("light");localStorage.setItem("autodash-light",document.body.classList.contains("light"))};
if(localStorage.getItem("autodash-light")==="true")document.body.classList.add("light");

$("#playBtn").onclick=()=>{playing=!playing;$("#playBtn").textContent=playing?"Ⅱ":"▶"};
$("#prev").onclick=()=>$("#trackTitle").textContent="Previous Track";
$("#next").onclick=()=>$("#trackTitle").textContent="Next Track";
$("#back10").onclick=()=>$("#progress").value=Math.max(0,+$("#progress").value-10);
$("#vol").onclick=()=>show("Volume","<p>Use your device's system volume controls.</p>");

function launch(url,fallback){
  let changed=false;
  try{window.location.href=url;changed=true}catch(e){}
  if(fallback)setTimeout(()=>{if(!document.hidden)window.location.href=fallback},1000);
  return changed;
}

function openMaps(query){
  const q=query?encodeURIComponent(query):"";
  const geo=q?`geo:0,0?q=${q}`:"geo:0,0";
  const web=q?`https://www.google.com/maps/search/?api=1&query=${q}`:"https://www.google.com/maps/";
  launch(geo,web);
}

function openMusic(){
  launch("spotify:","https://open.spotify.com/");
}

function openWeather(){
  launch("https://www.google.com/search?q=weather","https://weather.com/");
}

$("#navigateBtn").onclick=()=>{navActive=!navActive;$("#navigateBtn").textContent=navActive?"Stop navigation":"Start navigation";updateSpeed()};
$("#destinationBtn").onclick=()=>{show("Set destination",'<label>Destination</label><input id="dest" placeholder="Enter a place" autocomplete="street-address"><button class="action" id="saveDest">Open in Maps</button>');setTimeout(()=>$("#saveDest").onclick=()=>{const v=$("#dest").value.trim();if(v){$("#locationLabel").textContent="Route to "+v;closeModal();openMaps(v)}},0)};
$("#locBtn").onclick=()=>{if(!navigator.geolocation){$("#locationLabel").textContent="GPS not supported";return}navigator.geolocation.getCurrentPosition(p=>{$("#locationLabel").textContent=`GPS ${p.coords.latitude.toFixed(4)}, ${p.coords.longitude.toFixed(4)}`;openMaps(`${p.coords.latitude},${p.coords.longitude}`)},()=>$("#locationLabel").textContent="GPS permission unavailable",{enableHighAccuracy:true,timeout:10000,maximumAge:10000})};

if(navigator.getBattery)navigator.getBattery().then(b=>{const u=()=>{$("#battery").textContent=Math.round(b.level*100)+"%";$("#batteryState").textContent=b.charging?"Charging":"On battery"};u();b.addEventListener("levelchange",u);b.addEventListener("chargingchange",u)}).catch(()=>{});

function show(t,c){$("#modalTitle").textContent=t;$("#modalContent").innerHTML=c;$("#modal").classList.add("show")}
function closeModal(){$("#modal").classList.remove("show")}
$("#closeModal").onclick=closeModal;
$("#modal").onclick=e=>{if(e.target===$("#modal"))closeModal()};

function openApp(app){
  if(app==="Maps"){openMaps("");return}
  if(app==="Music"){openMusic();return}
  if(app==="Weather"){openWeather();return}
  if(app==="Browser"){launch("https://www.google.com/","https://www.google.com/");return}
  if(app==="Clock"){show("Clock",`<p style="font-size:44px;font-weight:800;text-align:center">${new Date().toLocaleTimeString()}</p>`);return}
  if(app==="Settings"){show("Settings","<p>Use the ☀ button for Light/Dark mode.</p><button class=\"action\" id=\"settingsTheme\">Toggle theme</button>");setTimeout(()=>$("#settingsTheme").onclick=()=>{$("#themeBtn").click();closeModal()},0);return}
  show(app,"<p>App unavailable.</p>");
}

$$('.app-grid button').forEach(b=>b.addEventListener('click',e=>{e.preventDefault();openApp(b.dataset.app)}));
$("#musicOpen").onclick=openMusic;
$("#allAppsBtn").onclick=()=>show("All Apps",'<div class="app-grid"><button data-app="Maps">⌖<span>Maps</span></button><button data-app="Music">♫<span>Music</span></button><button data-app="Weather">☁<span>Weather</span></button><button data-app="Settings">⚙<span>Settings</span></button><button data-app="Clock">◷<span>Clock</span></button><button data-app="Browser">◎<span>Browser</span></button></div>');
$("#modal").addEventListener("click",e=>{const b=e.target.closest("[data-app]");if(b)openApp(b.dataset.app)});

$$('.bottom-nav button').forEach(b=>b.onclick=()=>{$$('.bottom-nav button').forEach(x=>x.classList.remove('active'));b.classList.add('active');const p=b.dataset.page;if(p==='settings')openApp('Settings');else document.querySelector(p==='media'?'.media':p==='apps'?'.apps':p==='maps'?'.nav-card':'.dashboard').scrollIntoView({behavior:'smooth'})});
window.addEventListener('deviceorientation',e=>{if(typeof e.alpha==='number'){heading=Math.round(e.alpha);$("#heading").textContent=`Heading ${heading}°`;$("#needle").style.transform=`rotate(${heading}deg)`}});
