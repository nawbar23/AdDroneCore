import io
import socket
import struct
import time
import picamera

print ("Starting...")

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(('', 6667))
server_socket.listen(1)

camera = picamera.PiCamera()
camera.resolution = (720, 405)

time.sleep(2)

while True:

   try:
       print('waiting for connection')
       socket, client_addr = server_socket.accept()
       print("connection from ", client_addr)
       connection = socket.makefile('wb')
    
       stream = io.BytesIO()
       start = time.time()

       freqCounter = 0;
    
       for frame in camera.capture_continuous(stream, 'jpeg', use_video_port=True):
           print("sending frame ", stream.tell(), " freq: ", freqCounter / (time.time() - start), "Hz")
           connection.write(struct.pack('<L', stream.tell()))
           connection.flush()
        
           freqCounter += 1
         
           stream.seek(0)
           connection.write(stream.read())
        
           #if time.time() - start > 1200:
           #    break
			
		   time.sleep(0.088)
			
           stream.seek(0)
           stream.truncate()

       connection.write(struct.pack('<L', 0))
   except ConnectionResetError as e:
       print(type(e))
          
server_socket.close()

   
