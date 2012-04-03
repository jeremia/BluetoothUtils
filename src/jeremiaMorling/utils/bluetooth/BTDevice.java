/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jeremiaMorling.utils.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.lcdui.Image;
import jeremiaMorling.utils.managers.DM;
import jeremiaMorling.utils.managers.ResourceManager;
import jeremiaMorling.utils.vector.IClonable;
import jeremiaMorling.utils.vector.ISerializable;
import jeremiaMorling.utils.vector.ListItem;

/**
 * 
 *
 * @author Jeremia Mörling
 */
public class BTDevice extends ListItem implements ISerializable {
    //private DeviceClass deviceClass;
    
    private String address;
    private int majorDeviceClass = UNKNOWN;
    private String connectionURL;
    
    private RemoteDevice device;
    private ServiceRecord[] serviceRecords;
    private int transID = -1;
    
    private static Image mobileIcon;
    private static Image computerIcon;
    private static Image otherIcon;
    private static Image unknownIcon;

    public static final int COMPUTER = 0x00;
    public static final int MOBILE = 0x200;
    public static final int OTHER = -1;
    public static final int UNKNOWN = -2;
    
    static void loadIcons() {
        mobileIcon = ResourceManager.getImage( "/pictures/Mobile24.png" );
        computerIcon = ResourceManager.getImage( "/pictures/Computer24.png" );
        otherIcon = ResourceManager.getImage( "/pictures/Other24.png" );
        unknownIcon = ResourceManager.getImage( "/pictures/Unknown24.png" );
    }
    
    static void unloadIcons() {
        mobileIcon = null;
        computerIcon = null;
        otherIcon = null;
        unknownIcon = null;
    }
    
    public BTDevice(){}

    public BTDevice( RemoteDevice device ) {
        this( device, null );
    }

    public BTDevice( RemoteDevice device, DeviceClass deviceClass ) {
        setDevice( device );
        try {
            String deviceName = device.getFriendlyName( true );
            setText( deviceName );
        } catch ( IOException ex ) {
        }
        setDeviceClass( deviceClass );
    }
    
    private void setDevice( RemoteDevice device ) {
        this.device = device;
        address = device.getBluetoothAddress();
    }

    void setTransID( int transID ) {
        this.transID = transID;
    }

    int getTransID() {
        return transID;
    }
    
    ServiceRecord[] getServiceRecords() {
        return serviceRecords;
    }

    public void setServiceRecords( ServiceRecord[] serviceRecords ) {
        if( serviceRecords != null && serviceRecords.length > 0 )
            connectionURL = serviceRecords[0].getConnectionURL( ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false );
        this.serviceRecords = serviceRecords;
    }
    
    public String getConnectionURL() {
        return connectionURL;
    }
    
    public void setUrl( String url ) {
        this.connectionURL = url;
    }

    public int getMajorDeviceClass() {
        return majorDeviceClass;
    }
    
    public void setMajorDeviceClass( int majorDeviceClass ) {
            if( majorDeviceClass == MOBILE ||majorDeviceClass == COMPUTER )
                this.majorDeviceClass = majorDeviceClass;
            else if( majorDeviceClass != UNKNOWN )
                this.majorDeviceClass = OTHER;
            
            switch( this.majorDeviceClass ) {
                case MOBILE:
                    setIcon( mobileIcon );
                    break;
                case COMPUTER:
                    setIcon( computerIcon );
                    break;
                case OTHER:
                    setIcon( otherIcon );
                default:
                    setIcon( unknownIcon );
            }
    }
    
    public void setDeviceClass( DeviceClass deviceClass ) {
        if( deviceClass != null )
            setMajorDeviceClass( deviceClass.getMajorDeviceClass() );
        else
            setMajorDeviceClass( UNKNOWN );
    }

    /*public int getDeviceType() {
        if( majorDeviceClass == null || (majorDeviceClass.getMajorDeviceClass() != COMPUTER && majorDeviceClass.getMajorDeviceClass() != MOBILE) )
            return UNKNOWN;
        else
            return majorDeviceClass.getMajorDeviceClass();
    }*/

    public String getDeviceName() {
        return getText();
    }

    RemoteDevice getDevice() {
        return device;
    }

    public boolean equals( Object o ) {
        if( !(o instanceof BTDevice) )
            return false;

        BTDevice btDeviceToCompare = (BTDevice)o;
        return address.endsWith( btDeviceToCompare.address );
    }

    /*public int compareTo( IComparable comparable ) throws IllegalArgumentException {
        try {
            if ( comparable == null ) {
                throw new IllegalArgumentException( "comparable is null" );
            } else if ( !(comparable instanceof BTDevice) ) {
                throw new IllegalArgumentException( "comparable has to be of type BluetoothDevice, but is of type " + comparable.getClass().getName() );
            }
            BTDevice deviceToCompare = (BTDevice) comparable;
            return deviceName.toLowerCase().compareTo( deviceToCompare.deviceName.toLowerCase() );
        } catch ( Exception e ) {
            DM.error( e, "BluetoothDevice: compareTo()" );
            return 0;
        }
    }*/

    public IClonable clone() {
        try {
            BTDevice clone = new BTDevice( device );
            clone.setMajorDeviceClass( majorDeviceClass );
            clone.setTransID( transID );
            clone.setUrl( connectionURL );
            return clone;
        } catch ( Exception e ) {
            DM.error( e, "BluetoothDevice: clone()" );
            return null;
        }
    }
    
    public void toSerialFormat( DataOutputStream dataOutputStream ) {
        try {
            dataOutputStream.writeUTF( address );
            dataOutputStream.writeUTF( getText() );
            dataOutputStream.writeUTF( connectionURL );
            dataOutputStream.writeInt( majorDeviceClass );
        } catch( Exception e ) {
            DM.error( e, "BTDevice: toSerialFormat()" );
        }
    }
    
    public ISerializable fromSerialFormat( DataInputStream dataInputStream ) {
        try {
            address = dataInputStream.readUTF();
            setText( dataInputStream.readUTF() );
            connectionURL = dataInputStream.readUTF();
            setMajorDeviceClass( dataInputStream.readInt() );
        } catch( Exception e ) {
            DM.error( e, "BTDevice: fromSerialFormat()" );
        }
        
        return this;
    } 
}
