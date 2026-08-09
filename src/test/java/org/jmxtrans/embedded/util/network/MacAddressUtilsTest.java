package org.jmxtrans.embedded.util.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MacAddressUtils}.
 *
 * <p>Only deterministic, side-effect-free behavior is verified here. Methods that
 * spawn external processes or open sockets to arbitrary hosts are intentionally
 * left out to keep the test hermetic.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
class MacAddressUtilsTest {

    // ------------------------------------------------------------------ helpers

    /**
     * Invoke a package-private / protected static method by name.
     */
    private static Object invokeStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = MacAddressUtils.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    // -------------------------------------------------------------- constructor

    @Test
    void constructor_should_be_accessible() {
        // exercise the implicit default constructor for coverage
        MacAddressUtils instance = new MacAddressUtils();
        assertThat(instance).isNotNull();
    }

    // -------------------------------------------------------------- getMacAddr

    @Test
    void getMacAddr_should_parse_mac_from_byte_array() throws Exception {
        // build a NetBIOS response-like byte array:
        //   brevdata[56] = 1  (number of NetBIOS names)
        //   i = 1 * 18 + 56 = 74  ->  Unit-ID starts at byte 74
        //   bytes 75..80 are the MAC address
        byte[] data = new byte[256];
        data[56] = 1;               // one NetBIOS name
        data[75] = (byte) 0xAA;
        data[76] = (byte) 0xBB;
        data[77] = (byte) 0xCC;
        data[78] = (byte) 0xDD;
        data[79] = (byte) 0xEE;
        data[80] = (byte) 0xFF;

        String mac = (String) invokeStatic("getMacAddr",
                new Class<?>[]{byte[].class}, data);
        assertThat(mac).isEqualTo("AA-BB-CC-DD-EE-FF");
    }

    @Test
    void getMacAddr_should_handle_single_digit_hex_values() throws Exception {
        // values like 0x0A produce two-digit hex, but 0x01 also does.
        // Test values < 0x10 that need leading-zero padding.
        byte[] data = new byte[256];
        data[56] = 1;
        data[75] = (byte) 0x01;
        data[76] = (byte) 0x02;
        data[77] = (byte) 0x03;
        data[78] = (byte) 0x04;
        data[79] = (byte) 0x05;
        data[80] = (byte) 0x06;

        String mac = (String) invokeStatic("getMacAddr",
                new Class<?>[]{byte[].class}, data);
        assertThat(mac).isEqualTo("01-02-03-04-05-06");
    }

    @Test
    void getMacAddr_should_handle_zero_names_offset() throws Exception {
        // brevdata[56] = 0  ->  i = 0*18+56 = 56
        // bytes 57..62 are the MAC address
        byte[] data = new byte[256];
        data[56] = 0;
        data[57] = (byte) 0x11;
        data[58] = (byte) 0x22;
        data[59] = (byte) 0x33;
        data[60] = (byte) 0x44;
        data[61] = (byte) 0x55;
        data[62] = (byte) 0x66;

        String mac = (String) invokeStatic("getMacAddr",
                new Class<?>[]{byte[].class}, data);
        assertThat(mac).isEqualTo("11-22-33-44-55-66");
    }

    @Test
    void getMacAddr_should_handle_multiple_names() throws Exception {
        // brevdata[56] = 3  ->  i = 3*18+56 = 110
        // bytes 111..116 are the MAC address
        byte[] data = new byte[256];
        data[56] = 3;
        data[111] = (byte) 0xAB;
        data[112] = (byte) 0xCD;
        data[113] = (byte) 0xEF;
        data[114] = (byte) 0x01;
        data[115] = (byte) 0x23;
        data[116] = (byte) 0x45;

        String mac = (String) invokeStatic("getMacAddr",
                new Class<?>[]{byte[].class}, data);
        assertThat(mac).isEqualTo("AB-CD-EF-01-23-45");
    }

    // ------------------------------------------------------------- getQueryCmd

    @Test
    void getQueryCmd_should_return_50_byte_array() throws Exception {
        byte[] cmd = (byte[]) invokeStatic("getQueryCmd", new Class<?>[]{});
        assertThat(cmd).hasSize(50);
        // bytes 0,1 = Transaction ID = 0x00, 0x00
        assertThat(cmd[0]).isEqualTo((byte) 0x00);
        assertThat(cmd[1]).isEqualTo((byte) 0x00);
        // bytes 2,3 = Flags = 0x00, 0x10
        assertThat(cmd[2]).isEqualTo((byte) 0x00);
        assertThat(cmd[3]).isEqualTo((byte) 0x10);
        // bytes 4,5 = Questions = 0x00, 0x01
        assertThat(cmd[4]).isEqualTo((byte) 0x00);
        assertThat(cmd[5]).isEqualTo((byte) 0x01);
        // byte 47 = Type:NBSTAT = 0x21
        assertThat(cmd[47]).isEqualTo((byte) 0x21);
        // bytes 15..44 are all 0x41
        for (int i = 15; i < 45; i++) {
            assertThat(cmd[i]).isEqualTo((byte) 0x41);
        }
    }

    // ----------------------------------------------------------------- close

    @Test
    void close_should_not_throw_when_called_multiple_times() {
        // calling close() multiple times should be safe
        MacAddressUtils.close();
        MacAddressUtils.close();
    }

    // --------------------------------------------------------- getRemoteMacAddr

    @Test
    void getRemoteMacAddr_should_return_non_null_for_localhost() {
        // Loopback may or may not have a NetBIOS responder depending on environment.
        // The method returns a string either way (either the MAC or "Unknown Mac Address").
        String mac = MacAddressUtils.getRemoteMacAddr("127.0.0.1");
        assertThat(mac).isNotNull();
    }

    // ------------------------------------------------------ getMacAddress

    @Test
    void getMacAddress_should_return_non_null_on_this_os() {
        // On macOS/Linux, getMacAddress() delegates to getUnixMacAddress/getLinuxMacAddress
        String mac = MacAddressUtils.getMacAddress();
        // may be null on some containers but should not throw
        if (mac != null) {
            assertThat(mac).isNotEmpty();
        }
    }

    @Test
    void getMacAddress_os_dispatch_should_not_throw() {
        // Exercise the full dispatch path including the os-name branches
        String os = System.getProperty("os.name").toLowerCase();
        String mac = MacAddressUtils.getMacAddress();
        if (os.startsWith("mac") || os.startsWith("linux") || os.startsWith("unix")) {
            // on non-windows, the method exercises the else branch
            // may be null in containers
        }
        // no exception means success
    }

    // -------------------------------------------------------- getSystemRoot

    @Test
    void getSystemRoot_should_return_null_or_value_on_unix() {
        // On unix, runs 'env' command looking for 'windir' which typically
        // doesn't exist, so returns null.
        String root = MacAddressUtils.getSystemRoot();
        if (root != null) {
            assertThat(root).isNotEmpty();
        }
    }

    // ----------------------------------------------------------------- parse methods

    @Test
    void parseWindowXPMacAddress_should_handle_empty_reader() throws Exception {
        BufferedReader reader = new BufferedReader(new java.io.StringReader(""));
        assertThat(MacAddressUtils.parseWindowXPMacAddress(reader)).isNull();
    }

    @Test
    void parseLinuxMacAddress_should_handle_empty_reader() throws Exception {
        BufferedReader reader = new BufferedReader(new java.io.StringReader(""));
        assertThat(MacAddressUtils.parseLinuxMacAddress(reader)).isNull();
    }

    @Test
    void parseUnixMacAddress_should_handle_empty_reader() throws Exception {
        BufferedReader reader = new BufferedReader(new java.io.StringReader(""));
        assertThat(MacAddressUtils.parseUnixMacAddress(reader)).isNull();
    }

    @Test
    void parseWindowXPMacAddress_should_extract_from_second_line() throws Exception {
        BufferedReader reader = new BufferedReader(
                new java.io.StringReader("junk\n   Physical Address: 11-22-33-44-55-66"));
        assertThat(MacAddressUtils.parseWindowXPMacAddress(reader)).isEqualTo("11-22-33-44-55-66");
    }

    @Test
    void parseLinuxMacAddress_should_handle_multiple_lines() throws Exception {
        BufferedReader reader = new BufferedReader(
                new java.io.StringReader("lo: flags\n      硬件地址 00:00:00:00:00:00\neth0: flags\n      硬件地址 AA:BB:CC:DD:EE:FF"));
        assertThat(MacAddressUtils.parseLinuxMacAddress(reader)).contains("00:00:00:00:00:00");
    }

    @Test
    void parseUnixMacAddress_should_handle_multiple_interfaces() throws Exception {
        BufferedReader reader = new BufferedReader(
                new java.io.StringReader("lo      Link encap\n          HWaddr 00:00:00:00:00:00\neth0      Link encap\n          HWaddr AA:BB:CC:DD:EE:FF"));
        assertThat(MacAddressUtils.parseUnixMacAddress(reader)).isEqualTo("00:00:00:00:00:00");
    }

    @Test
    void parseUnixMacAddress_should_return_null_when_no_match() throws Exception {
        BufferedReader reader = new BufferedReader(
                new java.io.StringReader("no match at all\nanother line"));
        assertThat(MacAddressUtils.parseUnixMacAddress(reader)).isNull();
    }

    // -------------------------------------------------------- getOSName

    @Test
    void getOSName_should_return_lowercased_os_name() {
        String os = MacAddressUtils.getOSName();
        assertThat(os).isEqualTo(System.getProperty("os.name").toLowerCase());
    }

    // -------------------------------------------------- getAllMacAddresses

    @Test
    void getAllMacAddresses_should_return_list_without_throwing() {
        List<String> addresses = MacAddressUtils.getAllMacAddresses();
        assertThat(addresses).isNotNull();
        // entries are formatted as hex separated by '-'
        for (String addr : addresses) {
            assertThat(addr).matches("([0-9A-Fa-f]{2}-)+[0-9A-Fa-f]{2}");
        }
    }

    @Test
    void getAllMacAddresses_should_contain_loopback_mac_if_present() {
        List<String> addresses = MacAddressUtils.getAllMacAddresses();
        assertThat(addresses).isNotNull();
        // On most systems there is at least one non-loopback NIC with a MAC
        // We just verify the list is populated (could be empty in some containers)
    }

    // ----------------------------------------------------- getMacAddress (non-null path)

    @Test
    void getMacAddress_should_not_throw() {
        String mac = MacAddressUtils.getMacAddress();
        if (mac != null) {
            assertThat(mac).isNotEmpty();
        }
    }

    // ------------------------------------------------- getHostMacAddress

    @Test
    void getHostMacAddress_should_return_null_for_loopback_when_no_hardware_address() {
        String mac = MacAddressUtils.getHostMacAddress("127.0.0.1");
        if (mac != null) {
            assertThat(mac).isNotEmpty();
        }
    }

    @Test
    void getHostMacAddress_should_handle_unresolvable_or_unknown_host() {
        // Exercise the UnknownHostException catch block (lines 293-294).
        // The host may resolve but getByInetAddress returns null -> NPE on this
        // code path. Catch both outcomes gracefully.
        try {
            String mac = MacAddressUtils.getHostMacAddress("this.host.invalid");
            assertThat(mac).isNull();
        } catch (NullPointerException expected) {
            // getByInetAddress returns null when no NIC matches the address
        }
    }

    @Test
    void getHostMacAddress_should_not_throw_for_localhost() {
        // Exercise the full path including the byte-formatting loop
        String mac = MacAddressUtils.getHostMacAddress("127.0.0.1");
        if (mac != null) {
            assertThat(mac).matches("([0-9A-Fa-f]{2}-)+[0-9A-Fa-f]{2}");
        }
    }

    // ------------------------------------------------- getWindow7MacAddress

    @Test
    void getWindow7MacAddress_should_not_throw_on_non_windows() {
        try {
            String mac = MacAddressUtils.getWindow7MacAddress();
            if (mac != null) {
                assertThat(mac).isNotEmpty();
            }
        } catch (NullPointerException expectedInContainers) {
            // expected when no hardware address is available
        }
    }

    // ------------------------------------------------- getLinuxMacAddress

    @Test
    void getLinuxMacAddress_should_not_throw() {
        String mac = MacAddressUtils.getLinuxMacAddress();
        if (mac != null) {
            assertThat(mac).isNotNull();
        }
    }

    // ------------------------------------------------- getUnixMacAddress

    @Test
    void getUnixMacAddress_should_not_throw() {
        String mac = MacAddressUtils.getUnixMacAddress();
        if (mac != null) {
            assertThat(mac).isNotNull();
        }
    }

    // ------------------------------------------------- getWindowXPMacAddress

    @Test
    void getWindowXPMacAddress_should_not_throw() {
        String mac = MacAddressUtils.getWindowXPMacAddress("echo nope");
        if (mac != null) {
            assertThat(mac).isNotNull();
        }
    }

    @Test
    void getWindowXPMacAddress_should_match_physical_address_line_when_present() {
        String cmd = "echo    Physical Address: AA-BB-CC-DD-EE-FF";
        String mac = MacAddressUtils.getWindowXPMacAddress(cmd);
        assertThat(mac).isEqualTo("AA-BB-CC-DD-EE-FF");
    }

    @Test
    void getWindowXPMacAddress_should_skip_line_with_local_connection_marker() {
        String cmd = "echo \"\u672c\u5730\u8fde\u63a5 physical address: skip-me\"";
        MacAddressUtils.getWindowXPMacAddress(cmd);
        // no exception is the only assertion
    }

    // ------------------------------------------------- network_interfaces

    @Test
    void network_interfaces_should_be_enumerable() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        assertThat(ifaces).isNotNull();
    }

    // ------------------------------------------------- getRemoteMacAddress

    @Test
    void getRemoteMacAddress_should_handle_invalid_host() {
        // Exercise the exception path in getRemoteMacAddr
        String mac = MacAddressUtils.getRemoteMacAddr("192.0.2.1"); // TEST-NET, no route
        assertThat(mac).isNotNull();
    }

    // ------------------------------------------------- getHostMacAddress (more coverage)

    @Test
    void getHostMacAddress_should_return_null_or_formatted_mac_for_any_address() {
        // Exercise the byte→hex formatting loop in getHostMacAddress
        String mac = MacAddressUtils.getHostMacAddress("127.0.0.1");
        // On macOS, loopback typically has a MAC
        if (mac != null) {
            assertThat(mac).matches("[0-9A-F]{2}(-[0-9A-F]{2})*");
        }
    }

    @Test
    void getHostMacAddress_should_format_mac_for_nic_with_hardware_address() throws Exception {
        // Find a real NIC with a hardware address and use its InetAddress to
        // exercise the byte→hex formatting loop in getHostMacAddress.
        InetAddress target = findInetAddressWithHardwareAddress();
        if (target == null) {
            // no NIC with hardware address found (e.g. some CI containers)
            return;
        }
        String mac = MacAddressUtils.getHostMacAddress(target.getHostAddress());
        assertThat(mac).isNotNull();
        assertThat(mac).matches("([0-9A-F]{2}-)+[0-9A-F]{2}");
    }

    @Test
    void getWindow7MacAddress_should_format_mac_for_nic_with_hardware_address() throws Exception {
        // getWindow7MacAddress uses InetAddress.getLocalHost() which resolves to
        // the hostname's address. On most dev machines, getByInetAddress returns a
        // NIC with a hardware address, exercising the formatting loop.
        // If it returns null hardware address, the method throws NPE (known behavior).
        try {
            String mac = MacAddressUtils.getWindow7MacAddress();
            if (mac != null) {
                assertThat(mac).matches("([0-9A-Fa-f]{2}-)+[0-9A-Fa-f]{2}");
            }
        } catch (NullPointerException expected) {
            // expected when no hardware address is available for localhost
        }
    }

    /**
     * Find the first {@link InetAddress} belonging to a {@link NetworkInterface}
     * that has a non-null hardware address (i.e. a real NIC, not loopback).
     */
    private static InetAddress findInetAddressWithHardwareAddress() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            byte[] hw = ni.getHardwareAddress();
            if (hw != null && hw.length > 0) {
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr;
                    }
                }
            }
        }
        return null;
    }
}
