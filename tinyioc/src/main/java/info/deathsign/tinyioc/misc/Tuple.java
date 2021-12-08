package info.deathsign.tinyioc.misc;

public class Tuple<T,K,L> {
    T _t;
    K _k;
    L _l;
    public Tuple(T t, K k, L l){
        this._k = k;
        this._l = l;
        this._t = t;
    }

    public T get1(){
        return _t;
    }

    public K get2(){
        return _k;
    }

    public L get3(){
        return _l;
    }
}
