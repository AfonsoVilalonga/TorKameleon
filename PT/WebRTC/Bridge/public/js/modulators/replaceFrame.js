var seg_number = 1;

var last_rcv = 0;
var last_sent = 1;

var queue = {};

function encondeReplace(encodedFrame, controller) {    
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {

        var is_last_seg = false;

        const oldview = new DataView(encodedFrame.data);
        const keyframeBit = oldview.getUint8(0) & 0x01;
        const frameTagSize = (keyframeBit == 1) ? 3 : 10; 
        
        var to_encode = enconding[0];
        var num_of_bytes_encoded = 0;
        var num_of_bytes_to_encode = to_encode.length;

        const newData = new ArrayBuffer(encodedFrame.data.byteLength);
        const newView = new DataView(newData);

        for(let i = 0; i < frameTagSize; i++){
            newView.setUint8(i, oldview.getUint8(i));
        }

        for (let i = 0 ; i < num_of_bytes_to_encode && (i + frameTagSize) < encodedFrame.data.byteLength-12; i++) {
            newView.setUint8(i+frameTagSize, to_encode.shift());
            num_of_bytes_encoded = num_of_bytes_encoded + 1;
        }


        if(to_encode.length == 0){
            enconding.splice(0, 1);
            is_last_seg = true;
        }

        
        newView.setUint32(newData.byteLength - 12, last_sent);
        newView.setUint16(newData.byteLength - 8, seg_number);

        newView.setUint16(newData.byteLength - 5, num_of_bytes_encoded);
        newView.setUint16(newData.byteLength - 3, 12345);

        if(is_last_seg){
            last_sent = last_sent + 1;
            seg_number = 1;
            newView.setUint8(newData.byteLength - 6, 1);
        }else{
            seg_number = seg_number + 1;
            newView.setUint8(newData.byteLength - 6, 0);
        }
           
        encodedFrame.data = newData;
    }
    
    controller.enqueue(encodedFrame);
}

function decodeReplace(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame) {
        const view = new DataView(encodedFrame.data);
        
        const packet_id = view.getUint32(encodedFrame.data.byteLength-12);
        const seg_number = view.getUint16(encodedFrame.data.byteLength-8);
        const is_last = view.getUint8(encodedFrame.data.byteLength-6);
        const len = view.getUint16(encodedFrame.data.byteLength-5);
        const hasencoded = view.getUint16(encodedFrame.data.byteLength-3);

        if (!(encodedFrame.type === prevFrameType &&
                encodedFrame.timestamp === prevFrameTimestamp &&
                encodedFrame.synchronizationSource === prevFrameSynchronizationSource) && hasencoded == 12345) {

            const keyframeBit = view.getUint8(0) & 0x01;
            const frameTagSize = (keyframeBit == 1) ? 3 : 10; 
                
            var bytes = []

            for (let i = 0; i < len; i++) {
                bytes.push(view.getUint8(i + frameTagSize));
            }

            var q = queue[packet_id];

            if(q == undefined){
                q = [];
                queue[packet_id] =  q;
            }
              
            q[seg_number] = bytes;
            
            if(is_last == 1){
                q[q.length] = "end";
            }
            
            let x = last_rcv + 1;
            let stop = false;

            while(stop == false){
                let aux = queue[x];
                let send_pck = [];

                if(aux == undefined){
                    stop = true;
                }else{
                    for(let i = 1; i < aux.length-1; i++){
                        if(aux[i] != undefined){
                            send_pck.concat(aux[i])
                        }else{
                            stop = true;
                            break;
                        }

                        if(aux[i+1] == "end" && stop == false){
                            tor_conn.send(decode(send_pck));
                            last_rcv = last_rcv + 1;
                        }
                        
                        if(aux[i+1] != "end" && (i+1) == (aux.length-1)){
                            stop = true;
                            break;
                        }     
                    }
                }

                x = x + 1;
            }
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