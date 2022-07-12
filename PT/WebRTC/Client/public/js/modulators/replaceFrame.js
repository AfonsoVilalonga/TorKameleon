function encondeReplace(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {

        const oldview = new DataView(encodedFrame.data);
        const keyframeBit = oldview.getUint8(0) & 0x01;
        const frameTagSize = (keyframeBit == 1) ? 3 : 10; 

        var to_encode = enconding[0];
        console.log("enviei " + to_encode.length);
        enconding.splice(0, 1);

        const newData = new ArrayBuffer(encodedFrame.data.byteLength + 2 + 2);
        const newView = new DataView(newData);
        
        for(let i = 0; i < frameTagSize; i++){
            newView.setUint8(i, oldview.getUint8(i));
        }

        for (let i = frameTagSize ; i < to_encode.length; i++) {
            newView.setUint8(i, to_encode[i]);
        }

        for(let i = frameTagSize + to_encode.length; i < newData.byteLength - 4; i++){
            newView.setUint8(i, 0);
        }

        newView.setUint16(to_encode.length, to_encode.length);
        newView.setUint16(to_encode.length + 2, 12345);

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

            console.log("enviei " + len);

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