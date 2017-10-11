new Promise((resolve, reject) => {
    let r:string = $requestable$; let m:string = $marker$;
    let rm:string = r + (m != '' ? '#':'')+ m;
    this.call(rm,this.merge({__localCache_priority: $cachePolicy$, __localCache_ttl: $cacheTtl$},{/*=c8o_Vars*/}),null,500)
    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
})
.then((res:any) => {
/*=c8o_Then*/
}, (error: any) => {console.log("[MB] CallSequence : ", error.message);throw new Error(error);})