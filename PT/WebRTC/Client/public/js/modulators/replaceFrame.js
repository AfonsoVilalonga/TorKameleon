function encondeReplacing(encodedFrame) {
    var to_encode = enconding[0];

    if(encodedFrame.data.byteLength < to_encode.length){
        var to_encode = to_encode.splice(0, encodedFrame.data.byteLength);
    }else{
        enconding.splice(0, 1);
    }

    const newData = new ArrayBuffer(encodedFrame.data.byteLength + to_encode.length + 2 + 2);
    const newView = new DataView(newData);
    const oldData = new DataView(encodedFrame.data);

    console.log(to_encode.length);

    for (let i = 0; i < to_encode.length; i++) {
        newView.setUint8(i + encodedFrame.data.byteLength, to_encode[i]);
        newView.setUint8(i, oldData.getUint8(i));
    }

    newView.setUint16(encodedFrame.data.byteLength + to_encode.length, to_encode.length);
    newView.setUint16(encodedFrame.data.byteLength + to_encode.length + 2, 12345);
    return newData;
}

function decodeReplacing(encodedFrame, len){
    var bytes = []

    for (let i = encodedFrame.data.byteLength - 4 - len; i < encodedFrame.data.byteLength - 4; i++) {
        bytes.push(view.getUint8(i));
    }

    return decode(bytes);
}

function decode(bytes){
    var result = btoa(bytes.map(function(v){return String.fromCharCode(v)}).join(''))
    return result;
}