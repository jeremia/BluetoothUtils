package jeremiaMorling.utils.bluetooth;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import jeremiaMorling.utils.managers.Loc;

/**
 * @author Jeremia
 */
public class BTLoc extends Loc {
    private static BTLoc instance;
    
    private static BTLoc getInstance() {
        if( instance == null )
            instance = new BTLoc();
        
        return instance;
    }
    
    private BTLoc() {
        super( "/jeremiaMorling/utils/bluetooth/messages.properties" );
    }
    
    public static String getText( String key ) {
        return getInstance().internalGetText( key );
    }
}