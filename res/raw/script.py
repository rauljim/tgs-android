import sys
sys.stderr = open('/sdcard/swift/dht.err', 'w')
sys.stdout = open('/sdcard/swift/dht.out', 'w')

import android,time
import dht

droid = android.Android()
while 1:
    droid.makeToast('Python says: Hello, Android!')
    droid.vibrate(300)
    time.sleep(2)

dht.SwiftTraker(9999).start()
# Raul, 2012-03-09: SwiftTracker does not create a thread!!
