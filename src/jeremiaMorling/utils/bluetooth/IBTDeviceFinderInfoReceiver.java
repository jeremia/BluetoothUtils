/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jeremiaMorling.utils.bluetooth;

/**
 *
 * @author Jeremia
 */
public interface IBTDeviceFinderInfoReceiver {
    public void knownDevices( BTDeviceList deviceList );
    public void foundDevice( BTDevice bluetoothDevice, boolean isNewDevice );
    public void foundService( BTDevice bluetoothDevice );
    public void removeDevice( BTDevice bluetoothDevice );
    public void finishedDeviceSearch( BTDeviceList deviceList );
}
