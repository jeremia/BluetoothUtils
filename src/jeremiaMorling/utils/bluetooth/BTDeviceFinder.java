/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jeremiaMorling.utils.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.rms.RecordStoreException;
import jeremiaMorling.utils.managers.DM;
import jeremiaMorling.utils.vector.Queue;
import org.klings.wireless.BluetoothNumbers.BTServiceAttributeId;
import org.klings.wireless.BluetoothNumbers.BTServiceClass;

/**
 * 
 *
 * @author Jeremia Mörling
 */
public class BTDeviceFinder implements Runnable, DiscoveryListener {
    private Thread processThread;
    private DiscoveryAgent discoveryAgent;
    private BTDeviceList deviceList;
    private BTDeviceList selectedDevicesHistory;

    private static final UUID[] UUID_OPP = new UUID[] { new UUID( BTServiceClass.OBEXOBJECTPUSH ) };
    private static final int[] ATTR_SUPPORTED_FORMATS_LIST = new int[]{ BTServiceAttributeId.OPP_SUPPORTEDFORMATSLIST };
    public static final long OPP_FORMAT_V_CALENDAR_1_0 = 0x03;

    private UUID[] uuidSet;
    private int[] attrSet;
    private long oppFormat = -1;

    private int maxTransactions;
    private BTDevice prioritizedWaitingTransaction;
    private TransactionQueue waitingTransactions;
    private BTDeviceList activeTransactions;
    private boolean inquiryCompleted;

    private IBTDeviceFinderInfoReceiver bluetoothInfoReceiver;

    private static final int MAX_HISTORY_SIZE = 5;

    public BTDeviceFinder( IBTDeviceFinderInfoReceiver bluetoothInfoReceiver, long oppFormat ) {
        this( bluetoothInfoReceiver, UUID_OPP, ATTR_SUPPORTED_FORMATS_LIST );
        this.oppFormat = oppFormat;
    }

    public BTDeviceFinder( IBTDeviceFinderInfoReceiver bluetoothInfoReceiver, UUID[] uuidSet, int[] attrSet ) {
        this.bluetoothInfoReceiver = bluetoothInfoReceiver;
        this.uuidSet = uuidSet;
        this.attrSet = attrSet;

        processThread = new Thread( this );
        processThread.start();
    }

    public void run() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();
            discover();
        } catch( BluetoothStateException bse ) {
        } catch( Exception e ) {
            DM.error( e, "BTDeviceFinder: run()" );
        }
        
    }

    private void discover() throws BluetoothStateException, Exception {
        deviceList = new BTDeviceList();
        //bluetoothDeviceTable = new BluetoothDeviceTable();
        maxTransactions = Integer.parseInt( LocalDevice.getProperty( "bluetooth.sd.trans.max" ) );
        //transactionCounter = 0;
        activeTransactions = new BTDeviceList();
        waitingTransactions = new TransactionQueue();
        //serviceSearchCompletedCounter = 0;
        inquiryCompleted = false;
        addKnownDevices();
        discoveryAgent.startInquiry( DiscoveryAgent.GIAC, this );
    }

    private void addKnownDevices() throws BluetoothStateException, Exception {
        selectedDevicesHistory = BTDeviceList.loadPersisted();
        if( selectedDevicesHistory.size() > 0 )
            bluetoothInfoReceiver.knownDevices( selectedDevicesHistory );
        else {
            RemoteDevice[] cachedDevices = discoveryAgent.retrieveDevices( DiscoveryAgent.CACHED );
            if( cachedDevices != null )
                addDevices( cachedDevices );
        }

        RemoteDevice[] preknownDevices = discoveryAgent.retrieveDevices( DiscoveryAgent.PREKNOWN );
        if( preknownDevices != null )
            addDevices( preknownDevices );
    }

    private void addDevices( RemoteDevice[] devices ) throws BluetoothStateException, IOException {
        for( int i=0; i<devices.length; i++ )
            foundDevice( new BTDevice( devices[i] ) );
    }

    /* Searches for services of interested on remoted devices */
    /*private void searchServices( RemoteDevice[] deviceList ) throws BluetoothStateException {
        // Search the device list for our service of interest.
        for (int i = 0; i < deviceList.length; i++)
            searchService( deviceList[i] );
    }*/

    /*private void searchService( RemoteDevice remoteDevice ) throws BluetoothStateException {
        if( isDeviceAlreadyInList( remoteDevice ) )
            return;

        int transactionId = discoveryAgent.searchServices( SERVICE_NAME_ATTRIBUTE, OPP_UUID, remoteDevice, this );
        serviceSearchTransactions.addElement( new ServiceSearchTransaction( remoteDevice, transactionId ) );
    }*/

    /*private boolean isDeviceAlreadyInList( RemoteDevice remoteDevice ) {
        for( int i=0; i<serviceSearchTransactions.size(); i++ ) {
            if( getServiceSearchTransaction( i ).device.equals( remoteDevice ) )
                return true;
        }

        return false;
    }*/

    //private int counter;
    private boolean foundDevice( BTDevice btDevice ) throws BluetoothStateException {
        synchronized( this ) {
            //int counter = this.counter++;
            //DisplayManager.log( "foundDevice " + counter );
            //DisplayManager.log( bluetoothDevice.toString() );

            if( btDevice.getDeviceName() == null )
                return false;

            boolean isNewDevice = deviceList.addBluetoothDevice( btDevice );
            if( btDevice.getDeviceName() != null )
                bluetoothInfoReceiver.foundDevice( btDevice, isNewDevice );

            if( isNewDevice ) {
                if( activeTransactions.size() >= maxTransactions )
                    waitingTransactions.push( btDevice );
                else
                    searchForServices( btDevice );
            }

            //DisplayManager.log( "foundDevice exit" + counter );
            return isNewDevice;
        }
    }

    private void searchForServices( BTDevice btDevice ) throws BluetoothStateException {
        int transID = discoveryAgent.searchServices( attrSet, uuidSet, btDevice.getDevice(), this );
        btDevice.setTransID( transID );
        activeTransactions.addElement( btDevice );
    }

    public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) {
        try {
            foundDevice( new BTDevice( btDevice, cod ) );
        } catch ( Exception e ) {
            DM.error( e, "BluetoothDeviceFinder: deviceDiscovered()" );
        }
    }

    public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) {
        synchronized( this ) {
            //int counter = this.counter++;
            //DisplayManager.log( "servicesDiscovered " + counter );
            try {
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: servicesDiscovered(), addServiceRecord()" );
                BTDevice btDevice = activeTransactions.addServiceRecords( transID, servRecord );
                //DisplayManager.log( bluetoothDevice.toString() );
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: servicesDiscovered(),  bluetoothInfoReceiver.foundService( bluetoothDevice )" );
                bluetoothInfoReceiver.foundService( btDevice );
            } catch ( Exception e ) {
                //cancel();
                DM.error( e, "BluetoothDeviceFinder: servicesDiscovered()" );
            }
            //DisplayManager.log( "servicesDiscovered exit" + counter );
        }
    }

    public void serviceSearchCompleted( int transID, int respCode ) {
        synchronized( this ) {
            //int counter = this.counter++;
            //DisplayManager.log( "serviceSearchCompleted " + counter );
            try {
                if( respCode == DiscoveryListener.SERVICE_SEARCH_TERMINATED )
                    return;

                // Check if service search was successful
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: serviceSearchCompleted(), activeTransactions.getBluetoothDeviceWithTransID( transID )" );
                BTDevice btDevice = activeTransactions.getBluetoothDeviceWithTransID( transID );
                if( btDevice == null )
                    return;
                //DisplayManager.log( bluetoothDevice.toString() );
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: serviceSearchCompleted(), activeTransactions.removeElement( bluetoothDevice )" );
                activeTransactions.removeElement( btDevice );
                if ( respCode != DiscoveryListener.SERVICE_SEARCH_COMPLETED ||
                        (oppFormat != -1 && !hasCorrectOPPSupportedFormat( btDevice )) ) {
                    //DisplayManager.log( "Removing device " + respCode );
                    //ExceptionManager.PHASE( "BluetoothDeviceFinder: serviceSearchCompleted(), bluetoothInfoReceiver.removeDevice( bluetoothDevice );" );
                    bluetoothInfoReceiver.removeDevice( btDevice );
                }

                // Begin next service search
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: serviceSearchCompleted(), Begin next service search" );
                if( activeTransactions.size() < maxTransactions ) {
                    if ( prioritizedWaitingTransaction != null && !btDevice.equals( prioritizedWaitingTransaction ) ) {
                        searchForServices( prioritizedWaitingTransaction );
                        waitingTransactions.removeElement( prioritizedWaitingTransaction );
                        //prioritizedWaitingTransaction = null;
                    }
                    else if( !waitingTransactions.isEmpty() )
                        searchForServices( waitingTransactions.popBluetoothDevice() );
                }

                // Prioritized BluetoothDevice has been taken care of now
                if( prioritizedWaitingTransaction != null )
                    prioritizedWaitingTransaction = null;

                // Check if inquiry is completed
                //ExceptionManager.PHASE( "BluetoothDeviceFinder: serviceSearchCompleted(), Check if inquiry is completed" );
                if ( inquiryCompleted && activeTransactions.size() == 0 )
                    inquiryAndSearchesCompleted();
                
            } catch ( Exception e ) {
                DM.error( e, "BluetoothDeviceFinder: serviceSearchCompleted()" );
            }
            //DisplayManager.log( "serviceSearchCompleted exit" + counter );
        }
    }

    public void inquiryCompleted( int discType ) {
        synchronized( this ) {
            try {
                //int counter = this.counter++;
                //DisplayManager.log( "inquiryCompleted " + counter );
                inquiryCompleted = true;
                deviceList.setDiscType( discType );
                if( activeTransactions.size() == 0 )
                    inquiryAndSearchesCompleted();
                //DisplayManager.log( "inquiryCompleted exit " + counter );
            } catch( Exception e ) {
                DM.error( e, "BTDeviceFinder: inquiryCompleted" );
            }
        }
    }
    
    private boolean hasCorrectOPPSupportedFormat( BTDevice btDevice ) {
        //ExceptionManager.PHASE( "BluetoothDeviceFinder: hasCorrectOPPSupportedFormat()" );
        if( btDevice.getServiceRecords() == null )
            return false;

        ServiceRecord[] serviceRecords = btDevice.getServiceRecords();
        for( int i=0; i<serviceRecords.length; i++ ) {
            DataElement supportedFormatsDataElement = serviceRecords[i].getAttributeValue( BTServiceAttributeId.OPP_SUPPORTEDFORMATSLIST );
            Enumeration supportedFormatsEnumeration = (Enumeration)supportedFormatsDataElement.getValue();
            while( supportedFormatsEnumeration.hasMoreElements() ) {
                long supportedFormat = ((DataElement)supportedFormatsEnumeration.nextElement()).getLong();
                if( supportedFormat == oppFormat )
                    return true;
            }
        }

        return false;
    }

    private void inquiryAndSearchesCompleted() throws RecordStoreException, IOException {
        deviceList = deviceList.removeInvalidDevices();
        if( deviceList.getDiscType() != DiscoveryListener.INQUIRY_TERMINATED )
            bluetoothInfoReceiver.finishedDeviceSearch( deviceList );
        //devicesFromLastSearch = deviceList;
        //deviceList.persist();
    }

    public void serviceMissing( BTDevice btDevice ) {
        prioritizedWaitingTransaction = btDevice;
    }

    public void deviceSelected( BTDevice selectedDevice ) throws RecordStoreException, IOException {
        // Persist selectedDevicesHistory
        boolean previouslySelected = selectedDevicesHistory.removeElement( selectedDevice );
        if( !previouslySelected && selectedDevicesHistory.size() == MAX_HISTORY_SIZE )
            selectedDevicesHistory.removeLastElement();
        
        selectedDevicesHistory.insertElementFirst( selectedDevice );
        selectedDevicesHistory.persist();
    }
    
    public void cancel() {
        discoveryAgent.cancelInquiry( this );

        for( int i=0; i<activeTransactions.size(); i++ )
            discoveryAgent.cancelServiceSearch( activeTransactions.getBluetoothDevice( i ).getTransID() );

        activeTransactions.removeAllElements();
    }

    /*private class WaitingTransaction {
        private RemoteDevice device;
        private DeviceClass deviceClass;

        private WaitingTransaction( RemoteDevice device, DeviceClass deviceClass ) {
            this.device = device;
            this.deviceClass = deviceClass;
        }
    }*/

    /*private class BluetoothDeviceTable extends Hashtable {
        public BluetoothDevice getBluetoothDevice( int transID ) {
            return (BluetoothDevice)get( new Integer( transID ) );
        }

        public void putBluetoothDevice( int transID, BluetoothDevice bluetoothDevice ) {
            put( new Integer( transID ), bluetoothDevice );
        }
    }*/

    private class TransactionQueue extends Queue {
        public synchronized BTDevice popBluetoothDevice() {
            return (BTDevice)pop();
        }
    }
}
