const CACHE_NAME='autodash-v2';
const BASE='/ANDROID-AUTO/';
const APP_SHELL=[BASE,BASE+'index.html',BASE+'style.css',BASE+'script.js',BASE+'manifest.json',BASE+'icon.svg'];

self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const cache=await caches.open(CACHE_NAME);
    await Promise.all(APP_SHELL.map(async url=>{try{await cache.add(url)}catch(e){}}));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await Promise.all(keys.filter(k=>k!==CACHE_NAME).map(k=>caches.delete(k)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch',event=>{
  if(event.request.method!=='GET')return;
  const url=new URL(event.request.url);
  if(url.origin!==self.location.origin)return;
  event.respondWith((async()=>{
    const cached=await caches.match(event.request);
    if(cached)return cached;
    try{
      const response=await fetch(event.request);
      if(response.ok){const cache=await caches.open(CACHE_NAME);cache.put(event.request,response.clone());}
      return response;
    }catch(e){
      return caches.match(BASE+'index.html');
    }
  })());
});

self.addEventListener('message',event=>{if(event.data==='SKIP_WAITING')self.skipWaiting()});
