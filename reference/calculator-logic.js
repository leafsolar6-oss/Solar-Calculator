(function(){
 var root=document.getElementById('leafCalc');if(!root)return;
 // surge = multiplier applied to running watts for startup/peak; 1 = no surge
 var APPS=[
  {n:'Light Bulbs',b:'LB',def:0,surge:1,invToggle:false},
  {n:'Ceiling Fan',b:'CF',def:0,surge:3,invToggle:true},
  {n:'Television',b:'TV',def:0,surge:1,invToggle:false},
  {n:'Fridge / Freezer',b:'FR',def:0,surge:3,invToggle:true},
  {n:'Air Conditioner',b:'AC',def:0,surge:3,invToggle:true},
  {n:'Decoder / Router',b:'DR',def:0,surge:1,invToggle:false},
  {n:'Laptop / Phone',b:'LP',def:0,surge:1,invToggle:false},
  {n:'Sound System',b:'SS',def:0,surge:1,invToggle:false},
  {n:'Standing Fan',b:'SF',def:0,surge:3,invToggle:true},
  {n:'Microwave',b:'MW',def:0,surge:2,invToggle:false},
  {n:'Water Pump',b:'WP',def:0,surge:3,invToggle:false},
  {n:'Washing Machine',b:'WM',def:0,surge:3,invToggle:false}
 ];
 // Motor/compressor/heating words for custom appliances. Value = default surge multiplier.
 var SURGE_MAP=[
  [/\b(fan|fridge|freezer|refrigerator|ac|air.?cond|pump|washing|washer|compressor|motor|blender|grinder|drill|saw|sewing|vacuum|dryer|dishwasher|extractor|generator|sewing machine)\b/i,3],
  [/\b(microwave|oven|heater|kettle|toaster|iron|press|geyser|boiler|induction|hot\s?plate)\b/i,2]
 ];
 function detectSurge(name){
  for(var i=0;i<SURGE_MAP.length;i++){if(SURGE_MAP[i][0].test(name)){return SURGE_MAP[i][1];}}
  return 1;
 }
 var state={};
 APPS.forEach(function(a){state[a.n]={on:a.def>0,qty:a.def,w:0,badge:a.b,surge:a.surge,baseSurge:a.surge,isInverter:false};});
 var custom=[];
 var appsEl=root.querySelector('#lcApps');

 APPS.forEach(function(a){
  var d=document.createElement('div');d.className='lc-app'+(a.def>0?' on':'');d.setAttribute('data-n',a.n);
  var ic=document.createElement('span');ic.className='ic';ic.textContent=a.b;

  var nm=document.createElement('span');nm.className='nm';
  var label=document.createElement('span');label.className='nml';label.textContent=a.n;
  nm.appendChild(label);

  if(a.invToggle){
   var tg=document.createElement('span');tg.className='lc-invtog';
   tg.innerHTML='<label class="lc-switch"><input type="checkbox"><span class="lc-slider"></span></label><em>Inverter model</em>';
   var cb=tg.querySelector('input');
   cb.addEventListener('change',function(){
    state[a.n].isInverter=cb.checked;
    // inverter fridge/AC has soft start -> no surge
    state[a.n].surge=cb.checked?1:a.baseSurge;
    d.classList.toggle('is-inverter',cb.checked);
    render();
   });
   nm.appendChild(tg);
  }

  var win=document.createElement('span');win.className='win';
  var wi=document.createElement('input');wi.type='number';wi.min='1';wi.placeholder='W';wi.setAttribute('aria-label','Watts for '+a.n);
  win.appendChild(wi);nm.appendChild(win);
  d.appendChild(ic);d.appendChild(nm);

  var qty=document.createElement('span');qty.className='lc-qty';
  qty.innerHTML='<button type="button" data-act="minus" aria-label="Remove one">-</button><b data-q>0</b><button type="button" data-act="plus" aria-label="Add one">+</button>';
  d.appendChild(qty);appsEl.appendChild(d);

  var qtyEl=d.querySelector('[data-q]');
  function paint(){qtyEl.textContent=state[a.n].qty;d.classList.toggle('on',state[a.n].on);}
  wi.addEventListener('input',function(){state[a.n].w=parseFloat(wi.value)||0;if(state[a.n].w>0){state[a.n].on=true;d.classList.add('on');}render();});
  wi.addEventListener('focus',function(){d.classList.add('on');state[a.n].on=true;});
  d.querySelector('[data-act=minus]').addEventListener('click',function(e){e.stopPropagation();state[a.n].qty=Math.max(0,state[a.n].qty-1);if(state[a.n].qty<1){state[a.n].on=false;}paint();render();});
  d.querySelector('[data-act=plus]').addEventListener('click',function(e){e.stopPropagation();state[a.n].qty+=1;state[a.n].on=true;d.classList.add('on');paint();render();updateColHead();});
  paint();
 });

 var cl=root.querySelector('#lcCustomList');
 function addCustom(name,watts){
  if(!name){return;}
  var surge=detectSurge(name);
  var item={n:name,w:watts?+watts:0,surge:surge};
  custom.push(item);
  var row=document.createElement('div');row.className='lc-crow lc-cust';
  var ni=document.createElement('input');ni.value=name;ni.setAttribute('data-cn','');ni.placeholder='Appliance name';
  var wi=document.createElement('input');wi.type='number';wi.min='1';wi.value=watts||'';wi.placeholder='W';wi.setAttribute('data-cw','');
  var sc=document.createElement('label');sc.className='lc-surgecheck';sc.title='Starting current multiplier';
  function surgeLabel(m){return m>1?('x'+m):'x1';}
  sc.innerHTML='<input type="checkbox" '+(surge>1?'checked':'')+'> <span data-slabel>'+surgeLabel(surge)+'</span>';
  var cb=sc.querySelector('input');
  var sl=sc.querySelector('[data-slabel]');
  function setSurgeFromName(){item.surge=detectSurge(ni.value);cb.checked=item.surge>1;sl.textContent=surgeLabel(item.surge);}
  cb.addEventListener('change',function(){item.surge=cb.checked?3:1;sl.textContent=surgeLabel(item.surge);render();});
  var del=document.createElement('button');del.innerHTML='&#10005;';del.title='Remove';
  del.addEventListener('click',function(){var i=[].indexOf.call(cl.children,row);custom.splice(i,1);row.remove();render();});
  ni.addEventListener('input',function(){item.n=ni.value;setSurgeFromName();render();});
  wi.addEventListener('input',function(){item.w=+wi.value||0;render();});
  row.appendChild(ni);row.appendChild(wi);row.appendChild(sc);row.appendChild(del);
  cl.appendChild(row);
  render();
 }
 root.querySelector('#lcAdd').addEventListener('click',function(){
  addCustom(root.querySelector('#lcN').value.trim(),+root.querySelector('#lcW').value);
  root.querySelector('#lcN').value='';root.querySelector('#lcW').value='';
 });

 var hrs=root.querySelector('#lcHrs'),hrsVal=root.querySelector('#lcHrsVal');
 hrs.addEventListener('input',function(){hrsVal.innerHTML=' &mdash; '+hrs.value+' hrs';render();});

 var PACKAGES=[
  {kva:1.5,name:'1.5KVA Tubular Package',price:1200000,url:'/product-category/solar-inverters/solar-packages/tubular-packages/',battery:'1 x 220AH tubular battery',note:'Best for lights, fan, TV, phone charging and a small decoder.'},
  {kva:3.5,name:'3.5KVA Tubular Package',price:2300000,url:'/product-category/solar-inverters/solar-packages/tubular-packages/',battery:'2 x 220AH tubular batteries',note:'Adds a fridge, more fans and several outlets on a budget.'},
  {kva:3.5,name:'3.5KVA Lithium Package',price:4000000,url:'/product-category/solar-inverters/solar-packages/lithium-packages/',battery:'5kWh+ LiFePO4 battery',note:'Maintenance-free, 8-12 year battery life, faster charging.'},
  {kva:5,name:'5KVA Tubular Package',price:3800000,url:'/product-category/solar-inverters/solar-packages/tubular-packages/',battery:'4 x 220AH tubular batteries',note:'Handles most homes: fridge, TV, fans, lights, pumping.'},
  {kva:5,name:'5KVA Lithium Package',price:5200000,url:'/product-category/solar-inverters/solar-packages/lithium-packages/',battery:'10kWh+ LiFePO4 battery',note:'Quiet, long-lasting power for heavier household use.'},
  {kva:7.5,name:'7.5KVA Package',price:8500000,url:'/product-category/solar-inverters/solar-packages/lithium-packages/',battery:'15kWh lithium bank',note:'For larger homes with multiple ACs or heavy appliances.'},
  {kva:10,name:'10KVA Commercial Package',price:14800000,url:'/product-category/solar-inverters/solar-packages/commercial-packages/',battery:'Lithium bank + hybrid inverter',note:'For offices, shops, clinics and small businesses.'},
  {kva:20,name:'20KVA+ Industrial Package',price:24800000,url:'/product-category/solar-inverters/solar-packages/industrial-packages/',battery:'High-capacity lithium bank',note:'For factories, hotels and large complexes. Contact us for a custom design.'}
 ];
 function fmt(n){return '&#8358;'+Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g,',');}
 function roundKva(kw){var e=Math.ceil(kw*2)/2;if(e<0.5)e=0.5;return e;}

 function recommend(runningW,peakW){
  var exactKva=roundKva((peakW*1.2)/1000);
  var pick=PACKAGES[PACKAGES.length-1];
  for(var i=0;i<PACKAGES.length;i++){if(PACKAGES[i].kva>=exactKva){pick=PACKAGES[i];break;}}
  return {exactKva:exactKva,pick:pick};
 }

 function render(){
  var res=root.querySelector('#lcRes');
  var runningW=0,peakW=0,items=0,missing=[],surgeItems=[];
  APPS.forEach(function(a){
   var s=state[a.n];
   if(s.on){if(s.qty>0){
    var w=s.w||0;
    if(!w||w<1){missing.push(a.n);}
    else{
     items+=s.qty;
     var unitW=w*s.qty;
     runningW+=unitW;
     var mult=s.surge||1;
     peakW+=unitW*mult;
     if(mult>1){surgeItems.push(s.qty+'x '+a.n);}
    }
   }}
  });
  custom.forEach(function(c){
   if(c.n){
    if(!c.w||c.w<1){missing.push(c.n);}
    else{
     items++;
     runningW+=c.w;
     var m=c.surge||1;
     peakW+=c.w*m;
     if(m>1){surgeItems.push(c.n);}
    }
   }
  });

  if(runningW===0){
   if(missing.length===0){res.innerHTML='<div class="lc-empty">Tap + next to an appliance and type its wattage. Your estimate appears here.</div>';updateColHead();return;}
   res.innerHTML='<div class="lc-empty">Add the wattage (W) for: <b>'+missing.slice(0,4).join(', ')+'</b><br>Tap "How do I find the watt?" for help.</div>';
   updateColHead();return;
  }

  var h=+hrs.value;
  var wh=runningW*h;
  var rec=recommend(runningW,peakW);
  var ah=Math.round((wh*1.3)/12);
  var panels=Math.max(2,Math.ceil((runningW*h*0.7)/350));
  var waText=encodeURIComponent('Hello Leaf Solar! I used the load calculator. Running load: '+runningW+'W; startup (peak) load: '+peakW+'W; about '+h+' hrs/day. Recommended: '+rec.pick.name+' ('+rec.pick.price+' Naira). Please send a quote.');
  var warn='';
  if(missing.length){warn='<div class="lc-warn" style="display:block">Missing wattage for: '+missing.slice(0,4).join(', ')+' &mdash; not included.</div>';}
  var surgeNote=surgeItems.length?('<div class="lc-surgenote">Startup surge applied to: '+surgeItems.slice(0,5).join(', ')+(surgeItems.length>5?' & more':'')+'</div>'):'';
  res.innerHTML=warn
   +'<div class="lc-stat"><span class="k">Running load</span><span class="v">'+runningW+' W <small>/ '+items+' item'+(items>1?'s':'')+'</small></span></div>'
   +'<div class="lc-stat peak"><span class="k">Startup (peak) load</span><span class="v">'+peakW+' W</span></div>'
   +surgeNote
   +'<div class="lc-stat"><span class="k">Daily energy use</span><span class="v">'+wh+' Wh</span></div>'
   +'<div class="lc-stat"><span class="k">Inverter required</span><span class="v">'+rec.exactKva+' KVA</span></div>'
   +'<div class="lc-stat"><span class="k">Battery capacity</span><span class="v">&#8776; '+ah+' Ah <small>/ 12V</small></span></div>'
   +'<div class="lc-stat"><span class="k">Solar panels</span><span class="v">&#8776; '+panels+' x 350W</span></div>'
   +'<div class="lc-rec"><span class="tag">Recommended package</span>'
   +'<h3>'+rec.pick.name+'</h3>'
   +'<div><span class="price">'+fmt(rec.pick.price)+'</span><span class="from">from, installed</span></div>'
   +'<p>'+rec.pick.note+' Includes '+rec.pick.battery+', panels, inverter, delivery and installation.</p>'
   +'<a href="'+rec.pick.url+'">VIEW PACKAGE</a>'
   +'<a class="sec" target="_blank" rel="noopener" href="https://wa.me/2347037561216?text='+waText+'">GET A CUSTOM QUOTE</a></div>'
   +'<div class="lc-note">Motors and compressors (fans, fridges, pumps, non-inverter ACs, microwaves) draw extra current at startup, so the inverter is sized for the peak. Inverter fridges and inverter ACs use soft-start and have no surge. These figures are indicative; a free site assessment finalises the design. Prices include installation within Ibadan.</div>';
   updateColHead();
 }

 var how=root.querySelector('#lcHow'),howT=root.querySelector('#lcHowToggle'),colhead=root.querySelector('#lcColHead');
 if(howT){howT.addEventListener('click',function(){how.classList.toggle('open');howT.innerHTML=how.classList.contains('open')?'How do I find the watt? &#9650;':'How do I find the watt? &#9660;';});}
 function updateColHead(){if(!colhead)return;var any=document.querySelectorAll('#lcApps .lc-qty b');var show=false;for(var i=0;i<any.length;i++){if(parseInt((any[i].textContent||'0').replace(/[^0-9]/g,''),10)>0){show=true;break;}}colhead.style.display=show?'block':'none';}

 render();
})();