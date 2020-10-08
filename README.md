This app will detect face using ML Kit Face Detector by google

u have to input 2 photos.

it will detect 2 faces within the photos and crop the image to a small size so comparison is much easier.

after the crop, the images will be verified with facenet model inferenced to mobile. it will return a number (cos similarity). if that number is >0.4, the result is false (not verified), else, it will be true (verified)
