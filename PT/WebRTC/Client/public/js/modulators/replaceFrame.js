function encondeReplace(encodedFrame, controller) {    
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {
        const oldview = new DataView(encodedFrame.data);
        const keyframeBit = oldview.getUint8(0) & 0x01;
        const frameTagSize = (keyframeBit == 1) ? 3 : 10; 

        var covert_bytes = enconding[0];
        var to_encode = [];

        if(covert_bytes.length > encodedFrame.data.byteLength - frameTagSize - 4){
            while(covert_bytes.length > encodedFrame.data.byteLength - frameTagSize - 4){
                to_encode.push(covert_bytes.shift());
            }
        }else{
            to_encode = covert_bytes;
            enconding.splice(0, 1);
        }

        const newData = new ArrayBuffer(encodedFrame.data.byteLength);
        const newView = new DataView(newData);
    
        console.log(to_encode);

        for(let i = 0; i < frameTagSize; i++){
            newView.setUint8(i, oldview.getUint8(i));
        }

        for (let i = 0 ; i < to_encode.length; i++) {
            newView.setUint8(i+frameTagSize, to_encode[i]);
        }

        for(let i = frameTagSize + to_encode.length; i < newData.byteLength - 4; i++){
            newView.setUint8(i, oldview.getUint8(i));
        }

        console.log(newData);

        newView.setUint16(newData.byteLength - 4, to_encode.length);
        newView.setUint16(newData.byteLength - 2, 12345);

        encodedFrame.data = newData;
    }
    
    controller.enqueue(encodedFrame);
}

function decodeReplace(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame) {
        const view = new DataView(encodedFrame.data);
        
        const hasencoded = view.getUint16(encodedFrame.data.byteLength-2);
        const len = view.getUint16(encodedFrame.data.byteLength-4);

        if (!(encodedFrame.type === prevFrameType &&
                encodedFrame.timestamp === prevFrameTimestamp &&
                encodedFrame.synchronizationSource === prevFrameSynchronizationSource) && hasencoded == 12345) {

            const keyframeBit = view.getUint8(0) & 0x01;
            const frameTagSize = (keyframeBit == 1) ? 3 : 10; 
                
            var bytes = []

            for (let i = 0; i < len; i++) {
                bytes.push(view.getUint8(i + frameTagSize));
            }

            tor_conn.send(decode(bytes));
            encodedFrame.data = encodedFrame.data.slice(0, encodedFrame.data.byteLength - 4);
        }
        
        prevFrameType = encodedFrame.type;
        prevFrameTimestamp = encodedFrame.timestamp;
        prevFrameSynchronizationSource = encodedFrame.synchronizationSource;
    }

    controller.enqueue(encodedFrame);
}

function decode(bytes) {
    var result = btoa(bytes.map(function (v) {
        return String.fromCharCode(v)
    }).join(''))
    return result;
}