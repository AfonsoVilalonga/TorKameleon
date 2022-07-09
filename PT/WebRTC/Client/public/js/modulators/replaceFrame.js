function encondeReplace(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {
        var to_encode = enconding[0];
        var to_encode_copy = [];

        if(encodedFrame.data.byteLength < to_encode.length){
            for(let i = 0; i < encodedFrame.data.byteLength; i++){
                to_encode_copy.push(to_encode[i]);
                if(i + encodedFrame.data.byteLength < to_encode.length)
                    to_encode[i] = to_encode[i + encodedFrame.data.byteLength];
            }
        }else{
            enconding.splice(0, 1);
        }
        
        const newData = new ArrayBuffer(to_encode.length + 2 + 2);
        const newView = new DataView(newData);
        
        for (let i = 0; i < to_encode.length; i++) {
            newView.setUint8(i, to_encode[i]);
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
                
            var bytes = []

            for (let i = 0; i < encodedFrame.data.byteLength; i++) {
                    bytes.push(view.getUint8(i));
            }

            tor_conn.send(decode(bytes));
            encodedFrame.data = encodedFrame.data.slice(0, encodedFrame.data.byteLength - 4 - len);
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