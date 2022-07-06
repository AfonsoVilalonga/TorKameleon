class Transform{
    constructor(modulator, demodulator){
        this.modulator = modulator;
        this.demodulator = demodulator;
    }

    getModulator(){
        return this.modulator;
    }
    
    getDemodulator(){
        return this.demodulator;
    }
}