import {Injectable, OnDestroy } from '@angular/core';
import {Subject, Subscription} from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class Events implements OnDestroy{

    private eventMap = {};
    
    constructor() {
        
    }
    
    private getEventSubject(topic: string) : Subject<any> {
        if (topic == undefined || topic == '') {
            throw Error('Invalid topic');
        }
        
        if (this.eventMap[topic] == undefined) {
            this.eventMap[topic] = new Subject<any>()
        }
        return this.eventMap[topic];
    }
    
    public publish(topic: string, data?: any) {
        let subject = this.getEventSubject(topic)
        subject.next(data)
    }
    
    public subscribe(topic: string, next?: (value: any) => void, error?: (error: any) => any, complete?: () => void): Subscription {
        let subject = this.getEventSubject(topic)
        return subject.subscribe(next, error, complete)
    }
    
    ngOnDestroy() {
        for (const topic in this.eventMap) {
          this.eventMap[topic].complete()
        }
        this.eventMap = {};
    }    
}