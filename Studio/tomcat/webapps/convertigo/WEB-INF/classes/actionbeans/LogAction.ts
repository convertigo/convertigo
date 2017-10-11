Promise.resolve(console.log($message$))
.then((res:any) => {
/*=c8o_Then*/
}, (error: any) => {console.log("[MB] LogAction : ", error.message);throw new Error(error);})