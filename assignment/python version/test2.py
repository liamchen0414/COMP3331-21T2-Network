import threading
import time

                    
def wait_for_event(e):
    print('wait_for_event starting')
    event_is_set = e.wait()
    print('event set: %s', event_is_set)

def wait_for_event_timeout(e, t):
    while not e.isSet():
        print('wait_for_event_timeout starting')
        event_is_set = e.wait(t)
        print('event set: %s', event_is_set)
        if event_is_set:
            print('processing event')
        else:
            print('doing other things')

e = threading.Event()
t1 = threading.Thread(name='blocking', 
                    target=wait_for_event,
                    args=(e,))
t2 = threading.Thread(name='non-blocking', 
                    target=wait_for_event_timeout, 
                    args=(e, 5))

t1.start()
e.set()
t2.start()

# time.sleep(3)
# 
# print('Event is set')