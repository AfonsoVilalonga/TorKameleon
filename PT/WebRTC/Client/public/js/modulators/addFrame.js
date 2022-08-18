var last_rcv = 0;
var last_sent = 1;

var queue = {};


function encondeAdd(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {
        var to_encode = enconding[0];
        enconding.splice(0, 1);

        const newData = new ArrayBuffer(encodedFrame.data.byteLength + to_encode.length + 4 + 2 + 2);
        const newView = new DataView(newData);
        const oldData = new DataView(encodedFrame.data);

        for (let i = 0; i < to_encode.length; i++) {
            newView.setUint8(i + encodedFrame.data.byteLength, to_encode[i]);
        }
        for (let i = 0; i < encodedFrame.data.byteLength; i++) {
            newView.setUint8(i, oldData.getUint8(i));
        }
        newView.setUint32(encodedFrame.data.byteLength + to_encode.length, last_sent);
        newView.setUint16(encodedFrame.data.byteLength + to_encode.length + 4, to_encode.length);
        newView.setUint16(encodedFrame.data.byteLength + to_encode.length + 6, 12345);
        last_sent = last_sent + 1;
        encodedFrame.data = newData;
    }
    controller.enqueue(encodedFrame);
}


function decodeAdd(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame) {
        const view = new DataView(encodedFrame.data);

        const packet_id = view.getUint32(encodedFrame.data.byteLength - 8)
        const len = view.getUint16(encodedFrame.data.byteLength - 4);
        const hasencoded = view.getUint16(encodedFrame.data.byteLength - 2);

        if (!(encodedFrame.type === prevFrameType &&
            encodedFrame.timestamp === prevFrameTimestamp &&
            encodedFrame.synchronizationSource === prevFrameSynchronizationSource) && hasencoded == 12345) {

            var bytes = []

            for (let i = encodedFrame.data.byteLength - 8 - len; i < encodedFrame.data.byteLength - 8; i++) {
                bytes.push(view.getUint8(i));
            }

            if (packet_id == last_rcv + 1) {
                tor_conn.send(decode(bytes));
                last_rcv = last_rcv + 1;
            } else {
                queue[packet_id] = bytes;
                var queue_pack = queue[last_rcv + 1];
                while (queue_pack != undefined) {
                    tor_conn.send(decode(queue_pack));

                    delete queue[last_rcv];

                    last_rcv = last_rcv + 1;
                    queue_pack = queue[last_rcv + 1]
                }
            }

            encodedFrame.data = encodedFrame.data.slice(0, encodedFrame.data.byteLength - 8 - len);
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