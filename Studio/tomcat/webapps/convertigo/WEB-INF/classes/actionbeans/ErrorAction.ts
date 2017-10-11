Promise.reject($message$)
.then((res:any) => {
}, (error: any) => {console.log("[MB] ErrorAction");throw new Error(error);})