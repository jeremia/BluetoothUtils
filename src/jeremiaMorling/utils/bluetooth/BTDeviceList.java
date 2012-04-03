/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jeremiaMorling.utils.bluetooth;

import java.io.IOException;
import javax.bluetooth.ServiceRecord;
import javax.microedition.rms.RecordStoreException;
import jeremiaMorling.utils.vector.ListVector;
import jeremiaMorling.utils.vector.IClonable;

/**
 * 
 *
 * @author Jeremia Mörling
 */
public class BTDeviceList extends ListVector {
    private int discType;
    
    private static final String RS_KNOWN_BT_DEVICES = "knownBTDevices";

    public BTDevice getBluetoothDevice( int index ) {
        return (BTDevice)elementAt( index );
    }

    public boolean addBluetoothDevice( BTDevice bluetoothDevice ) {
        int index = indexOf( bluetoothDevice );
        if ( index != -1 ) {
            getBluetoothDevice( index ).setMajorDeviceClass( bluetoothDevice.getMajorDeviceClass() );
            return false;
        }
        else {
            super.addElement( bluetoothDevice );
            return true;
        }

        /*StringBuffer names = new StringBuffer();
        for ( int i = 0; i < size(); i++ ) {
            names.append( getBluetoothDevice( i ).getDeviceName() );
            names.append( ", " );
        }
        ExceptionManager.PHASE( names.toString() );*/
    }

    public BTDevice addServiceRecords( int transID, ServiceRecord[] servRecord ) throws IOException {
        //ExceptionManager.PHASE( "BluetoothDeviceList: addServiceRecords()" );
        if( servRecord == null ) {
            //ExceptionManager.PHASE( "BluetoothDeviceList: addServiceRecords(), servRecord == null" );
            return null;
        }

        //ExceptionManager.PHASE( "BluetoothDeviceList: addServiceRecords(), transID: " + transID );
        BTDevice bluetoothDevice = getBluetoothDeviceWithTransID( transID );
        if( bluetoothDevice == null ) {
            //ExceptionManager.PHASE( "BluetoothDeviceList: addServiceRecords(), device == null" );
            return null;
        }

        bluetoothDevice.setServiceRecords( servRecord );
        return bluetoothDevice;
    }

    public BTDevice getBluetoothDeviceWithTransID( int transID ) {
        for( int i=0; i<size(); i++ ) {
            //ExceptionManager.PHASE( "BluetoothDeviceList: getBluetoothDeviceWithTransID(), index: " + i + ", transID: " + getBluetoothDevice( i ).getTransID() );
            if( getBluetoothDevice( i ).getTransID() == transID )
                return getBluetoothDevice( i );
        }

        return null;
    }

    public void setDiscType( int discType ) {
        this.discType = discType;
    }

    public int getDiscType() {
        return discType;
    }

    public BTDeviceList removeInvalidDevices() {
        BTDeviceList newBluetoothDeviceList = new BTDeviceList();
        newBluetoothDeviceList.discType = discType;
        for( int i=0; i<size(); i++ ) {
            if( getBluetoothDevice( i ).getServiceRecords() != null )
                newBluetoothDeviceList.addElement( getBluetoothDevice( i ) );
        }

        return newBluetoothDeviceList;
    }

    public IClonable clone() {
        BTDeviceList clone = (BTDeviceList)super.clone();
        clone.discType = discType;
        return clone;
    }
    
    public void persist() throws RecordStoreException, IOException {
        persist( RS_KNOWN_BT_DEVICES );
        /*RecordStore rs = RecordStore.openRecordStore( RS_KNOWN_BT_DEVICES, true );
        
        // Delete any old records
        RecordEnumeration recordEnumeration = rs.enumerateRecords( null, null, false );
        while( recordEnumeration.hasNextElement() ) {
            int recordId = recordEnumeration.nextRecordId();
            rs.deleteRecord( recordId );
        }
        recordEnumeration.destroy();
        
        // Add new record
        if( size() == 0 )
            return;
        
        byte[] record;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream( outputStream );
        for( int i=0; i<size(); i++ )
            getBluetoothDevice( i ).toSerialFormat( dataOutputStream );
        
        dataOutputStream.flush();
        record = outputStream.toByteArray();
        rs.addRecord( record, 0, record.length );
        
        outputStream.close();
        dataOutputStream.close();
        rs.closeRecordStore();*/
    }
    
    public static BTDeviceList loadPersisted() throws RecordStoreException, IOException, InstantiationException, IllegalAccessException {
        BTDeviceList result = new BTDeviceList();
        result.loadPersisted( RS_KNOWN_BT_DEVICES, BTDevice.class );
        return result;
        /*RecordStore rs = RecordStore.openRecordStore( RS_KNOWN_BT_DEVICES, true );
        RecordEnumeration recordEnumeration = rs.enumerateRecords( null, null, false );

        if( !recordEnumeration.hasNextElement() )
            return null;

        BTDeviceList result = new BTDeviceList();
        byte[] record = recordEnumeration.nextRecord();
        ByteArrayInputStream inputStream = new ByteArrayInputStream( record );
        DataInputStream dataInputStream = new DataInputStream( inputStream );
        while( dataInputStream.available() > 0 )
            result.addElement( BTDevice.fromSerialFormat( dataInputStream ) );

        inputStream.close();
        dataInputStream.close();
        recordEnumeration.destroy();
        rs.closeRecordStore();

        return result;*/
    }
}
