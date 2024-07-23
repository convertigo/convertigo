import { writable } from 'svelte/store';
import { getUrl } from '$lib/utils/service';

export const appUrlStore = writable('');
export const qrCodeUrlStore = writable('');

function generateAppUrl(projectName) {
    let href = `/projects/${projectName}/DisplayObjects/mobile/index.html`;
    const fullUrl = getUrl(href);
    console.log(`Generated App URL: ${fullUrl}`);
    return fullUrl;
}

function generateQRCodeUrl(projectName) {
    let targetUrl = generateAppUrl(projectName);

    const qrUrl = `${getUrl('/qrcode')}?${new URLSearchParams({
        o: 'image/png',
        e: 'L',
        s: '3',
        d: targetUrl
    }).toString()}`;
    console.log(`Generated QR Code URL: ${qrUrl}`);
    return qrUrl;
}

export function updateUrls(projectName) {
    const appUrl = generateAppUrl(projectName);
    const qrCodeUrl = generateQRCodeUrl(projectName);
    appUrlStore.set(appUrl);
    qrCodeUrlStore.set(qrCodeUrl);
    console.log('App URL', appUrl);
    console.log('QR Code URL', qrCodeUrl);
}
