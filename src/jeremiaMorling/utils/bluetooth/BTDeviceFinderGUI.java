/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jeremiaMorling.utils.bluetooth;

import javax.bluetooth.UUID;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import jeremiaMorling.utils.displayUtils.displays.AlertDisplay;
import jeremiaMorling.utils.displayUtils.displays.SortableList;
import jeremiaMorling.utils.managers.DM;
import jeremiaMorling.utils.managers.ResourceManager;

/**
 * 
 *
 * @author Jeremia Mörling
 */
public abstract class BTDeviceFinderGUI extends SortableList implements IBTDeviceFinderInfoReceiver, CommandListener {
    private BTDeviceFinder bluetoothDeviceFinder;
    private BTDevice selectedBTDevice;

    private static final int ILLEGAL_DEVICE_ALERT_TIMEOUT = 2000;
    
    private BTDeviceFinderGUI() {
        super( BTLoc.getText( "searching" ), List.IMPLICIT );
        
        BTDevice.loadIcons();
        
        setItems( new BTDeviceList() );
        setSelectCommand( new Command( BTLoc.getText( "select" ), Command.OK, 0 ) );
        addCommand( new Command( BTLoc.getText( "cancel" ), Command.CANCEL, 1 ) );
        setCommandListener( this );
    }

    public BTDeviceFinderGUI( long oppFormat ) {
        this();
        bluetoothDeviceFinder = new BTDeviceFinder( this, oppFormat );
    }

    public BTDeviceFinderGUI( UUID[] uuidSet, int[] attrSet ) {
        this();
        bluetoothDeviceFinder = new BTDeviceFinder( this, uuidSet, attrSet );
    }

    private void initialize() {

    }

    /*public void refresh() throws IOException {
        deleteAll();
        //deviceList.sort();
        for( int i=0; i<deviceList.size(); i++ ) {
            BTDevice btDevice = deviceList.getBluetoothDevice( i );
            append( btDevice.getDeviceName(), getDeviceIcon( btDevice ) );
        }
    }*/

    /*private void refreshBluetoothDevice( int index ) throws IOException {
        BTDevice btDevice = deviceList.getBluetoothDevice( index );
        set( index, btDevice.getDeviceName(), getDeviceIcon( btDevice ) );
    }*/

    /*private Image getDeviceIcon( BTDevice btDevice ) throws IOException {
        Image result;

        int deviceType = btDevice.getDeviceType();
        switch( deviceType ) {
            case BTDevice.PHONE:
                result = mobileIcon;
                break;
            case BTDevice.COMPUTER:
                result = computerIcon;
                break;
            default:
                result = unknownIcon;
        }

        return result;
    }*/

    public void knownDevices( BTDeviceList deviceList ) {
        setItemsAndRefresh( deviceList );
    }

    public void foundDevice( BTDevice btDevice, boolean isNewDevice ) {
        int index = indexOf( btDevice );

        // Device alreay exists
        if( index != -1 ) {
            BTDevice bluetoothDeviceInList = (BTDevice)getItem( index );
            if( bluetoothDeviceInList.getMajorDeviceClass() == BTDevice.UNKNOWN ) {
                bluetoothDeviceInList.setMajorDeviceClass( btDevice.getMajorDeviceClass() );
                refresh( index );
                return;
            }
        }

        // New device
        else if( isNewDevice ) {
            append( btDevice );
        }
    }

    public void foundService( BTDevice btDevice ) {
        getDeviceInList( btDevice ).setServiceRecords( btDevice.getServiceRecords() );
        if( selectedBTDevice != null && btDevice.equals( selectedBTDevice ) ) {
            DM.back();
            properDeviceSelected();
        }
    }

    private BTDevice getDeviceInList( BTDevice btDevice ) {
        int index = indexOf( btDevice );
        if( index == -1 )
            return null;
        else
            return (BTDevice)getItem( index );
    }

    public void removeDevice( BTDevice btDevice ) {
        int index = indexOf( btDevice );
        if( index == -1 )
            return;
        if ( selectedBTDevice != null && btDevice.equals( selectedBTDevice ) ) {
            DM.back();
            DM.newAlert(
                    BTLoc.getText( "incompatibleDevice.title" ),
                    BTLoc.getText( "incompatibleDevice.text" ),
                    ResourceManager.getImage( "/pictures/Error64.png" ),
                    ILLEGAL_DEVICE_ALERT_TIMEOUT );
        }
        delete( index );
    }

    public void finishedDeviceSearch( BTDeviceList deviceList ) {
        setItemsAndRefreshSameSelection( deviceList );
        if( deviceList.size() == 0 )
            setTitle( BTLoc.getText( "noDevicesFound" ) );
        else
            setTitle( BTLoc.getText( "foundDevices" ) );
    }

    public void commandAction( Command c, Displayable d ) {
        int commandType = c.getCommandType();
        switch( commandType ) {
            case Command.OK:
                selectedBTDevice = (BTDevice)getSelectedItem();
                if( selectedBTDevice.getConnectionURL() == null ) {
                    bluetoothDeviceFinder.serviceMissing( selectedBTDevice );
                    DM.add( new AlertDisplay(
                            BTLoc.getText( "checkingCompatibility.title" ),
                            BTLoc.getText( "checkingCompatibility.text" ),
                            ResourceManager.getImage( "/pictures/Wait64.png" ) ) );
                }
                else {
                    properDeviceSelected();
                }
                break;
            case Command.CANCEL:
                DM.back();
                break;
        }
    }

    private void properDeviceSelected() {
        try {
            bluetoothDeviceFinder.cancel();
            deviceSelected( selectedBTDevice );
            bluetoothDeviceFinder.deviceSelected( selectedBTDevice );
            BTDevice.unloadIcons();
        } catch( Exception e ) {
            DM.error( e, "BTDeviceFinderGUI: properDeviceSelected()" );
        }
    }
    
    protected abstract void deviceSelected( BTDevice btDevice );
}
