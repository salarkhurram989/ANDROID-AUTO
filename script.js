const $=s=>document.querySelector(s), $$=s=>document.querySelectorAll(s);
let playing=false,speed=0,tripKm=0,tripSeconds=0,navActive=false,heading=0,deferredInstallPrompt=null;

function clock(){const d=new Date();$("#clock").textContent=d.toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"})}
setInterval(clock,1000);clock();

$("#themeBtn").onclick=()=>{document.body.classList.toggle("light");localStorage.setItem("autodash-light",document.body.classList.contains("light"))};
if(localStorage.getItem("autodash-light")==="true")document.body.classList.add("light");

// PWA install: Chrome supplies this event when the site is installable.
const installBtn=$("#installBtn");
window.addEventListener("beforeinstallprompt",e=>{
  e.preventDefault();
  deferredInstallPrompt=e;
  installBtn.hidden=false;
  installBtn.textContent="Install";
});
installBtn.onclick=async()=>{
  if(!deferredInstallPrompt){
    show("Install AutoDash","<p>To install AutoDash, open your browser menu and choose <b>Add to Home screen</b> or <b>Install app</b>.</p>");
    return;
  }
  deferredInstallPrompt.prompt();
  const result=await deferredInstallPrompt.userChoice;
  deferredInstallPrompt=null;
  installBtn.hidden=true;
};
window.addEventListener("appinstalled",()=>{
  deferredInstallPrompt=null;
  installBtn.hidden=true;
  $("#systemState").textContent="INSTALLED";
  $("#systemDetail").textContent="Home-screen app ready";
});

// Register the service worker for offline/app-shell caching.
if("serviceWorker" in navigator){
  window.addEventListener("load",async()=>{
    try{
      const registration=await navigator.serviceWorker.register("./sw.js",{scope:"./"});
      registration.addEventListener("updatefound",()=>{
        const worker=registration.installing;
        if(worker)worker.addEventListener("statechange",()=>{
          if(worker.state==="installed" && navigator.serviceWorker.controller){
            worker.postMessage("SKIP_WAITING");
          }
        });
      });
      $("#systemState").textContent="READY";
      $("#systemDetail").textContent="PWA + offline cache";
    }catch(e){
      $("#systemState").textContent="ONLINE";
      $("#systemDetail").textContent="Browser mode";
    }
  });
}

function updateSpeed(){$("#speed").textContent=Math.round(speed);$("#driveState").textContent=speed>2?"DRIVING":"PARKED"}
setInterval(()=>{speed=navActive?Math.min(62,speed+Math.random()*5):Math.max(0,speed-Math.random()*4);updateSpeed()},1200);
setInterval(()=>{if(navActive){tripSeconds++;if(speed>1)tripKm+=speed/3600}$("#tripTime").textContent=`${String(Math.floor(tripSeconds/60)).padStart(2,"0")}:${String(tripSeconds%60).padStart(2,"0")}`;$("#trip").textContent=tripKm.toFixed(1)+" km"},1000);

$("#playBtn").onclick=()=>{playing=!playing;$("#playBtn").textContent=playing?"Ⅱ":"▶"};
$("#prev").onclick=()=>$("#trackTitle").textContent="Previous Track";
$("#next").onclick=()=>$("#trackTitle").textContent="Next Track";
$("#back10").onclick=()=>$("#progress").value=Math.max(0,+$("#progress").value-10);
$("#vol").onclick=()=>show("Volume","<p>Use your device's system volume controls.</p>");

function openMaps(query){
  const q=query?encodeURIComponent(query):"";
  const url=q?`geo:0,0?q=${q}`:"geo:0,0";
  try{window.location.href=url}catch(e){}
  if(q)setTimeout(()=>{window.open(`https://www.google.com/maps/search/?api=1&query=${q}`,'_blank','noopener')},900);
}

$("#navigateBtn").onclick=()=>{navActive=!navActive;$("#navigateBtn").textContent=navActive?"Stop navigation":"Start navigation";updateSpeed()};
$("#destinationBtn").onclick=()=>{show("Set destination",'<label>Destination</label><input id="dest" placeholder="Enter a place" autocomplete="street-address"><button class="action" id="saveDest">Open in Maps</button>');setTimeout(()=>$("#saveDest").onclick=()=>{const v=$("#dest").value.trim();if(v){$("#locationLabel").textContent="Route to "+v;closeModal();openMaps(v)}},0)};
$("#locBtn").onclick=()=>{if(!navigator.geolocation){$("#locationLabel").textContent="GPS not supported";return}navigator.geolocation.getCurrentPosition(p=>{$("#locationLabel").textContent=`GPS ${p.coords.latitude.toFixed(4)}, ${p.coords.longitude.toFixed(4)}`;openMaps(`${p.coords.latitude},${p.coords.longitude}`)},()=>$("#locationLabel").textContent="GPS permission unavailable",{enableHighAccuracy:true,timeout:10000,maximumAge:10000})};

if(navigator.getBattery)navigator.getBattery().then(b=>{const u=()=>{$("#battery").textContent=Math.round(b.level*100)+"%";$("#batteryState").textContent=b.charging?"Charging":"On battery"};u();b.addEventListener("levelchange",u);b.addEventListener("chargingchange",u)});

function show(t,c){$("#modalTitle").textContent=t;$("#modalContent").innerHTML=c;$("#modal").classList.add("show")}
function closeModal(){$("#modal").classList.remove("show")}
$("#closeModal").onclick=closeModal;
$("#modal").onclick=e=>{if(e.target===$("#modal"))closeModal()};

function openMusic(){
  try{window.location.href="spotify:"}catch(e){}
  setTimeout(()=>window.open("https://open.spotify.com/",'_blank','noopener'),900)
}
function openApp(app){const pages={Maps:'<p>Open an installed navigation app.</p><button class="action" id="modalMaps">Open Maps</button>',Music:'<p>Open your installed music app.</p><button class="action" id="modalMusic">Open Music</button>',Weather:'<p>Live weather requires an internet weather service.</p>',Settings:'<p>Theme, installation and dashboard controls are available.</p>',Clock:`<p style="font-size:44px;font-weight:800">${new Date().toLocaleTimeString()}</p>`,Browser:'<p>Use your browser normally; this dashboard can also be installed as an app.</p>'};show(app,pages[app]||"<p>App unavailable</p>");if(app==="Maps")setTimeout(()=>$("#modalMaps").onclick=()=>openMaps(""),0);if(app==="Music")setTimeout(()=>$("#modalMusic").onclick=openMusic,0)}
$$('.app-grid button').forEach(b=>b.onclick=()=>openApp(b.dataset.app));
$("#musicOpen").onclick=openMusic;
$("#allAppsBtn").onclick=()=>show("All Apps","<p>Maps · Music · Weather · Settings · Clock · Browser</p>");
$$('.bottom-nav button').forEach(b=>b.onclick=()=>{$$('.bottom-nav button').forEach(x=>x.classList.remove('active'));b.classList.add('active');const p=b.dataset.page;if(p==='settings')openApp('Settings');else document.querySelector(p==='media'?'.media':p==='apps'?'.apps':p==='maps'?'.nav-card':'.dashboard').scrollIntoView({behavior:'smooth'})});
window.addEventListener('deviceorientation',e=>{if(typeof e.alpha==='number'){heading=Math.round(e.alpha);$("#heading").textContent=`Heading ${heading}°`;$("#needle").style.transform=`rotate(${heading}deg)`}});
