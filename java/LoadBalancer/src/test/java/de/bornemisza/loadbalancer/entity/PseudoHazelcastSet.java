package de.bornemisza.loadbalancer.entity;

import java.util.HashSet;

import com.hazelcast.core.ISet;
import com.hazelcast.core.ItemListener;

public class PseudoHazelcastSet<T> extends HashSet<T> implements ISet<T> {

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String addItemListener(ItemListener<T> il, boolean bln) {
        return "MyListener";
    }

    @Override
    public boolean removeItemListener(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPartitionKey() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getServiceName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
