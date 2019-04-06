import os
from flask import Flask, request, redirect, url_for, send_from_directory
from werkzeug.utils import secure_filename
import StringIO

UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

@app.route('/<path:filename>', methods=["GET"])
def image(filename):
    return send_from_directory('uploads', filename)

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # check if the post request has the file part
        if "file" not in request.files:
            print('~~~~~~ No file part ~~~~~~')
            print(request.files.to_dict())
            print("-------------------------------")
            return 'NA' #redirect(request.url)
        file = request.files['file']
        # if user does not select file, browser also
        # submit a empty part without filename
        if file.filename == '':
            print('No selected file')
            return 'EF'
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            p = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            print(p)
            file.save(p)
            print("Success")
            return 'OK'
    return 'FI'


if __name__ == '__main__':
   app.run(host = '0.0.0.0', port = 8282, debug = True)

