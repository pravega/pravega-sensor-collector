<!--
Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->
# Pravega Sensor Collector Accelerometer Driver

### Sample Output

```json
{
"TimestampsNanos":[1594506841105617708,1594506841106244794,1594506841106868543],
"X":[0.0,0.0,0.0],
"Y":[0.152983,0.152983,0.152983],
"Z":[9.943895000000001,9.943895000000001,10.096878],
"RemoteAddr":"gw3001"
}
```

### Enable accelerometer FIFO device
```bash
modprobe iio-trig-hrtimer
echo 0 > /sys/bus/iio/devices/iio:device3/buffer/enable
rmdir /sys/kernel/config/iio/triggers/hrtimer/pravega-sensor-collector1
mkdir /sys/kernel/config/iio/triggers/hrtimer/pravega-sensor-collector1
echo 1600 > /sys/bus/iio/devices/trigger0/sampling_frequency
echo 1600 > /sys/bus/iio/devices/iio:device3/sampling_frequency
echo pravega-sensor-collector1 > /sys/bus/iio/devices/iio:device3/trigger/current_trigger
echo 1 > /sys/bus/iio/devices/iio:device3/scan_elements/in_accel_x_en
echo 1 > /sys/bus/iio/devices/iio:device3/scan_elements/in_accel_y_en
echo 1 > /sys/bus/iio/devices/iio:device3/scan_elements/in_accel_z_en
echo 1 > /sys/bus/iio/devices/iio:device3/scan_elements/in_timestamp_en
echo 1000 > /sys/bus/iio/devices/iio:device3/buffer/length
echo 1 > /sys/bus/iio/devices/iio:device3/buffer/enable 
chmod a+r /dev/iio:device3
```

### Confirm enabled accelerometer FIFO device
```bash
cat /dev/iio:device3 | xxd -
```

### To disable accelerometer FIFO device
```bash
echo 0 > /sys/bus/iio/devices/iio:device3/buffer/enable 
rmdir /sys/kernel/config/iio/triggers/hrtimer/pravega-sensor-collector1
```

### Optional steps

```bash
sudo apt-get install libiio-utils i2c-tools
```

### Accelerometer Statistics

```
i.p.sensor.collector.Statistics: Since start: meanFreqHz=1586.031, meanMs=0.631, stdDevMs=0.060, minMs=0.397, maxMs=75.675, count=98732800
```

### Accelerometer Investigation and Troubleshooting Notes

lsmod output:
```
root@gw3001:~# lsmod
Module                  Size  Used by
8250_dw                16384  0
ad5592r_base           20480  1 ad5593r
ad5593r                16384  0
aes_x86_64             20480  1 aesni_intel
aesni_intel           188416  18
arc4                   16384  2
async_memcpy           16384  2 raid456,async_raid6_recov
async_pq               16384  2 raid456,async_raid6_recov
async_raid6_recov      20480  1 raid456
async_tx               16384  5 async_pq,async_memcpy,async_xor,raid456,async_raid6_recov
async_xor              16384  3 async_pq,raid456,async_raid6_recov
autofs4                40960  2
bluetooth             548864  25 bnep,btrsi
bnep                   20480  2
btrfs                1126400  0
btrsi                  16384  2 rsi_91x,rsi_sdio
ccm                    20480  24
cdc_acm                32768  4
cdc_mbim               16384  0
cdc_ncm                36864  1 cdc_mbim
cdc_wdm                20480  2 cdc_mbim
cfg80211              622592  2 rsi_91x,mac80211
cmac                   16384  1
coretemp               16384  0
crc32_pclmul           16384  0
crct10dif_pclmul       16384  0
cryptd                 24576  3 crypto_simd,ghash_clmulni_intel,aesni_intel
crypto_simd            16384  1 aesni_intel
dcdbas                 16384  1 dell_smbios
dell_smbios            24576  1 dell_wmi
dell_wmi               16384  0
dell_wmi_descriptor    16384  2 dell_wmi,dell_smbios
dw_dmac                16384  0
dw_dmac_core           24576  1 dw_dmac
ecdh_generic           24576  2 bluetooth
ghash_clmulni_intel    16384  0
glue_helper            16384  1 aesni_intel
hts221                 16384  1 hts221_i2c
hts221_i2c             16384  0
i2c_i801               28672  0
iTCO_vendor_support    16384  1 iTCO_wdt
iTCO_wdt               16384  1
ib_cm                  53248  1 rdma_cm
ib_core               225280  4 rdma_cm,iw_cm,ib_iser,ib_cm
ib_iser                49152  0
iio_trig_hrtimer       16384  3
industrialio           69632  13 st_pressure,industrialio_triggered_buffer,iio_trig_hrtimer,st_sensors,st_pressure_i2c,hts221_i2c,hts221,kfifo_buf,st_accel_i2c,ad5592r_base,st_accel
industrialio_configfs    16384  2 industrialio_sw_trigger
industrialio_sw_trigger    16384  1 iio_trig_hrtimer
industrialio_triggered_buffer    16384  3 st_pressure,hts221,st_accel
intel_cstate           20480  0
intel_powerclamp       16384  0
intel_soc_dts_iosf     16384  1 intel_soc_dts_thermal
intel_soc_dts_thermal    16384  0
ip_tables              28672  0
ipmi_devintf           20480  0
ipmi_msghandler        53248  1 ipmi_devintf
irqbypass              16384  1 kvm
iscsi_tcp              20480  0
iw_cm                  45056  1 rdma_cm
kfifo_buf              16384  1 industrialio_triggered_buffer
kvm                   598016  1 kvm_intel
kvm_intel             212992  0
libcrc32c              16384  1 raid456
libiscsi               53248  3 libiscsi_tcp,iscsi_tcp,ib_iser
libiscsi_tcp           20480  1 iscsi_tcp
linear                 16384  0
lpc_ich                24576  0
mac80211              782336  2 rsi_91x,rsi_sdio
mac_hid                16384  0
mei                    90112  1 mei_txe
mei_txe                20480  0
mii                    16384  2 r8169,usbnet
mmc_block              36864  4
multipath              16384  0
nls_iso8859_1          16384  1
punit_atom_debug       16384  0
pwm_lpss               16384  1 pwm_lpss_platform
pwm_lpss_platform      16384  0
r8169                  86016  0
raid0                  20480  0
raid1                  40960  0
raid10                 53248  0
raid456               143360  0
raid6_pq              114688  4 async_pq,btrfs,raid456,async_raid6_recov
rdma_cm                61440  1 ib_iser
rsi_91x                77824  1 rsi_sdio
rsi_sdio               28672  0
sch_fq_codel           20480  6
scsi_transport_iscsi    98304  3 iscsi_tcp,ib_iser,libiscsi
sdhci                  49152  1 sdhci_acpi
sdhci_acpi             16384  0
sparse_keymap          16384  1 dell_wmi
spi_pxa2xx_platform    24576  0
st_accel               20480  3 st_accel_i2c
st_accel_i2c           16384  0
st_pressure            16384  1 st_pressure_i2c
st_pressure_i2c        16384  0
st_sensors             20480  2 st_pressure,st_accel
st_sensors_i2c         16384  2 st_pressure_i2c,st_accel_i2c
usb_serial_simple      20480  0
usbnet                 45056  2 cdc_mbim,cdc_ncm
usbserial              45056  1 usb_serial_simple
video                  45056  1 dell_wmi
wdat_wdt               16384  0
wmi                    24576  4 dell_wmi,wmi_bmof,dell_smbios,dell_wmi_descriptor
wmi_bmof               16384  0
x_tables               40960  1 ip_tables
xor                    24576  2 async_xor,btrfs
xr_usb_serial_common    45056  0
zstd_compress         163840  1 btrfs
```

```bash
admin@gw3001:~$ ls -l -R "/sys/bus/iio/devices/iio:device3/"
'/sys/bus/iio/devices/iio:device3/':
total 0
drwxr-xr-x 2 root root    0 Jul 11 06:14 buffer
-rw-r--r-- 1 root root 4096 Jul 11 06:14 current_timestamp_clock
-r--r--r-- 1 root root 4096 Jul 11 06:14 dev
-r--r--r-- 1 root root 4096 Jul 11 06:14 in_accel_scale_available
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_x_raw
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_x_scale
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_y_raw
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_y_scale
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_z_raw
-rw-r--r-- 1 root root 4096 Jul 11 06:14 in_accel_z_scale
-r--r--r-- 1 root root 4096 Jul 11 06:14 name
drwxr-xr-x 2 root root    0 Jul 11 06:14 power
-rw-r--r-- 1 root root 4096 Jul 11 07:26 sampling_frequency
-r--r--r-- 1 root root 4096 Jul 11 06:14 sampling_frequency_available
drwxr-xr-x 2 root root    0 Jul 11 06:14 scan_elements
lrwxrwxrwx 1 root root    0 Jul 11 06:14 subsystem -> ../../../../../../bus/iio
drwxr-xr-x 2 root root    0 Jul 11 06:14 trigger
-rw-r--r-- 1 root root 4096 Jul 11 06:14 uevent

'/sys/bus/iio/devices/iio:device3/buffer':
total 0
-rw-r--r-- 1 root root 4096 Jul 11 14:53 enable
-rw-r--r-- 1 root root 4096 Jul 11 14:53 length
-rw-r--r-- 1 root root 4096 Jul 11 14:53 watermark

'/sys/bus/iio/devices/iio:device3/power':
total 0
-rw-r--r-- 1 root root 4096 Jul 11 06:15 async
-rw-r--r-- 1 root root 4096 Jul 11 06:15 autosuspend_delay_ms
-rw-r--r-- 1 root root 4096 Jul 11 06:15 control
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_active_kids
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_active_time
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_enabled
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_status
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_suspended_time
-r--r--r-- 1 root root 4096 Jul 11 06:15 runtime_usage

'/sys/bus/iio/devices/iio:device3/scan_elements':
total 0
-rw-r--r-- 1 root root 4096 Jul 11 14:53 in_accel_x_en
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_x_index
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_x_type
-rw-r--r-- 1 root root 4096 Jul 11 14:53 in_accel_y_en
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_y_index
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_y_type
-rw-r--r-- 1 root root 4096 Jul 11 14:53 in_accel_z_en
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_z_index
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_accel_z_type
-rw-r--r-- 1 root root 4096 Jul 11 14:53 in_timestamp_en
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_timestamp_index
-r--r--r-- 1 root root 4096 Jul 11 14:53 in_timestamp_type

'/sys/bus/iio/devices/iio:device3/trigger':
total 0
-rw-r--r-- 1 root root 4096 Jul 11 06:15 current_trigger

admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat name
lng2dm
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat sampling_frequency_available 
1 10 25 50 100 200 400 1600
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat current_timestamp_clock 
realtime
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat dev
240:3
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat sampling_frequency_available 
1 10 25 50 100 200 400 1600
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat uevent
MAJOR=240
MINOR=3
DEVNAME=iio:device3
DEVTYPE=iio_device
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat in_accel_scale_available 
0.152983 0.305967 0.612915 1.838746
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat in_accel_x_raw
1
admin@gw3001:/sys/bus/iio/devices/iio:device3$ cat in_accel_x_scale
0.152983
```

```bash
root@gw3001:/sys/bus/iio/devices/iio:device3# iio_info
Library version: 0.10 (git tag: v0.10)
Compiled with backends: local xml ip usb serial
IIO context created with local backend.
Backend version: 0.10 (git tag: v0.10)
Backend description string: Linux gw3001 4.15.0-1031-oem #36-Ubuntu SMP Mon Jan 7 09:40:58 UTC 2019 x86_64
IIO context has 1 attributes:
	local,kernel: 4.15.0-1031-oem
IIO context has 4 devices:
	iio:device3: lng2dm (buffer capable)
		4 channels found:
			accel_x:  (input, index: 0, format: le:S8/8>>0)
			3 channel-specific attributes found:
				attr  0: scale value: 0.152983
				attr  1: raw value: 1
				attr  2: scale_available value: 0.152983 0.305967 0.612915 1.838746
			accel_y:  (input, index: 1, format: le:S8/8>>0)
			3 channel-specific attributes found:
				attr  0: raw value: 1
				attr  1: scale value: 0.152983
				attr  2: scale_available value: 0.152983 0.305967 0.612915 1.838746
			accel_z:  (input, index: 2, format: le:S8/8>>0)
			3 channel-specific attributes found:
				attr  0: raw value: 64
				attr  1: scale value: 0.152983
				attr  2: scale_available value: 0.152983 0.305967 0.612915 1.838746
			timestamp:  (input, index: 3, format: le:S64/64>>0)
		3 device-specific attributes found:
				attr  0: sampling_frequency_available value: 1 10 25 50 100 200 400 1600
				attr  1: sampling_frequency value: 100
				attr  2: current_timestamp_clock value: realtime

		1 debug attributes found:
				debug attr  0: direct_reg_access ERROR: Operation not permitted (-1)
	iio:device1:
		1 channels found:
			temp:  (input)
			3 channel-specific attributes found:
				attr  0: scale value: 376.789750000
				attr  1: offset value: -753
				attr  2: raw value: 897
	iio:device2: hts221
		3 channels found:
			humidityrelative:  (input)
			4 channel-specific attributes found:
				attr  0: offset value: -10555.063829787
				attr  1: raw value: 791
				attr  2: scale value: -0.002937500
				attr  3: oversampling_ratio value: 32
			temp:  (input)
			5 channel-specific attributes found:
				attr  0: scale value: 0.017906250
				attr  1: oversampling_ratio value: 16
				attr  2: offset value: 1156.809773123
				attr  3: raw value: 1594
				attr  4: oversampling_ratio_available value: 2 4 8 16 32 64 128 256
			humidity:  (input)
			1 channel-specific attributes found:
				attr  0: oversampling_ratio_available value: 4 8 16 32 64 128 256 512
		3 device-specific attributes found:
				attr  0: sampling_frequency_available value: 1 7 13
				attr  1: sampling_frequency value: 1
				attr  2: current_timestamp_clock value: realtime

	iio:device0: lps22hb (buffer capable)
		3 channels found:
			pressure:  (input)
			2 channel-specific attributes found:
				attr  0: raw value: 4130951
				attr  1: scale value: 0.000024414
			temp:  (input)
			2 channel-specific attributes found:
				attr  0: scale value: 10.000000000
				attr  1: raw value: 5036
			timestamp:  (input, index: 2, format: le:S64/64>>0)
		3 device-specific attributes found:
				attr  0: sampling_frequency_available value: 1 10 25 50 75
				attr  1: sampling_frequency value: 1
				attr  2: current_timestamp_clock value: realtime

		1 debug attributes found:
				debug attr  0: direct_reg_access ERROR: Operation not permitted (-1)
```

/var/log/kern.log:
```
Jun 29 07:59:23 localhost kernel: [    6.115138] i801_smbus 0000:00:1f.3: SMBus using PCI interrupt
Jun 29 07:59:23 localhost kernel: [   13.526363] st-press-i2c i2c-SMO9210:00: i2c-SMO9210:00 supply vdd not found, using dummy regulator
Jun 29 07:59:23 localhost kernel: [   13.526415] st-press-i2c i2c-SMO9210:00: i2c-SMO9210:00 supply vddio not found, using dummy regulator
Jun 29 07:59:23 localhost kernel: [   13.527616] iio iio:device2: registered pressure sensor lps22hb
Jun 29 07:59:23 localhost kernel: [   13.533962] st-accel-i2c i2c-SMO8A90:00: i2c-SMO8A90:00 supply vdd not found, using dummy regulator
Jun 29 07:59:23 localhost kernel: [   13.534007] st-accel-i2c i2c-SMO8A90:00: i2c-SMO8A90:00 supply vddio not found, using dummy regulator
Jun 29 07:59:23 localhost kernel: [   13.535558] iio iio:device3: registered accelerometer lng2dm
```

```
modinfo iio-trig-hrtimer
filename:       /lib/modules/4.15.0-1031-oem/kernel/drivers/iio/trigger/iio-trig-hrtimer.ko
license:        GPL v2
description:    Periodic hrtimer trigger for the IIO subsystem
author:         Daniel Baluta <daniel.baluta@intel.com>
author:         Marten Svanfeldt <marten@intuitiveaerial.com>
srcversion:     F98F2B7577CDE7CC414F0A4
depends:        industrialio-sw-trigger,industrialio
retpoline:      Y
intree:         Y
name:           iio_trig_hrtimer
vermagic:       4.15.0-1031-oem SMP mod_unload 
signat:         PKCS#7
signer:         
sig_key:        
sig_hashalgo:   md4

root@gw3001:/sys/bus/iio/devices# ls -lh
total 0
lrwxrwxrwx 1 root root 0 Jul 11 15:59 iio:device0 -> ../../../devices/platform/80860F41:00/i2c-0/i2c-SMO9210:00/iio:device0
lrwxrwxrwx 1 root root 0 Jul 11 15:59 iio:device1 -> ../../../devices/platform/80860F41:02/i2c-2/i2c-ADS5593:00/iio:device1
lrwxrwxrwx 1 root root 0 Jul 11 15:59 iio:device2 -> ../../../devices/platform/80860F41:00/i2c-0/i2c-SMO9100:00/iio:device2
lrwxrwxrwx 1 root root 0 Jul 11 06:14 iio:device3 -> ../../../devices/platform/80860F41:00/i2c-0/i2c-SMO8A90:00/iio:device3

root@gw3001:/sys/bus/iio/devices# i2cdetect -l
i2c-3	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-1	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-6	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-4	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-2	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-0	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter
i2c-7	smbus     	SMBus I801 adapter at e000      	SMBus adapter
i2c-5	i2c       	Synopsys DesignWare I2C adapter 	I2C adapter

root@gw3001:/sys/bus/iio/devices# i2cdetect 0
Warning: Can't use SMBus Quick Write command, will skip some addresses
WARNING! This program can confuse your I2C bus, cause data loss and worse!
I will probe file /dev/i2c-0.
I will probe address range 0x03-0x77.
Continue? [Y/n] y
     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
00:                                                 
10:                                                 
20:                                                 
30: -- -- -- -- -- -- -- --                         
40:                                                 
50: -- -- -- -- -- -- -- -- -- -- -- -- UU -- -- UU 
60:                                                 
70:

iio_readdev -b 256 -s 1024 -a -T 10000 -t claudio1 iio:device3 | xxd
WARNING: High-speed mode not enabled
00000000: 0100 40ee 0961 9331 70a7 4f6b 80c3 2016  ..@..a.1p.Ok.. .
00000010: 0100 3fee 0961 9331 68d5 e46b 80c3 2016  ..?..a.1h..k.. .
...

root@gw3001:/sys/bus/iio/devices/trigger0# time iio_readdev -b 256 -s 1024 -a -T 10000 -t claudio1 iio:device3 accel_z | wc -c
WARNING: High-speed mode not enabled
1024
real	0m10.332s

echo 200 > /sys/bus/iio/devices/trigger0/sampling_frequency
cat /sys/bus/iio/devices/trigger0/sampling_frequency

cp /dev/iio:device3 /tmp/out
xxd /tmp/out
00000000: 0000 41ee 0961 9331 f857 9e0f c6c4 2016  ..A..a.1.W.... .
00000010: 0000 41ee 0961 9331 45a5 a70f c6c4 2016  ..A..a.1E..... .
00000020: 01ff 3fee 0961 9331 0d2b b10f c6c4 2016  ..?..a.1.+.... .

01ff 3f                x, y, z, raw unscaled, 8-bit signed integer each
0d2b b10f c6c4 2016    timestamp, 64-bit unsigned integer, nanoseconds since epoch
```

## References

- https://elinux.org/images/b/ba/ELC_2017_-_Industrial_IO_and_You-_Nonsense_Hacks%21.pdf
- https://www.kernel.org/doc/html/latest/driver-api/iio/intro.html
- https://dbaluta.github.io/iiobuffer.html
- https://www.oreilly.com/library/view/linux-device-drivers/9781785280009/7b80480e-2711-464d-96b6-a761ec43a6b2.xhtml
- https://oslongjourney.github.io/linux-kernel/experiment-three-iio-dummy/
- https://www.st.com/en/mems-and-sensors/lis2dh12.html#overview
- https://wiki.st.com/stm32mpu/wiki/IIO_overview
