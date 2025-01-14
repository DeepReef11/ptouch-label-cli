# ptouch-label

A simple script in Babashka to print beautiful labels with label printers like a ptouch (the script could easily be adjusted to other devices).

![Example](example.png)

## Requirement

For debian:

``` 
sudo apt install cutycapt
sudo apt install imagemagick
```

## Install


``` 
# Download, build and install ptouch-print if not already installed
sudo apt install libgd-dev gettext cmake libusb-1.0-0-dev
git clone https://git.familie-radermacher.ch/linux/ptouch-print.git
cd ptouch-print
./build.sh
ls -l build/ptouch-print 
build/ptouch-print --version
prefix=$HOME/software/ptouch-print
mkdir -p "$prefix"/{bin,share/man/man1}
cp build/ptouch-print "$prefix/bin/"
cp ptouch-print.1 "$prefix/share/man/man1"
sudo chmod u+s "$prefix/bin/ptouch-print"
sudo chown root "$prefix/bin/ptouch-print"

# Install

sudo cp build/ptouch-print /bin/

# You may need to do the following to have ptouch-print in env variable between sessions. You can remove zshenv (and/or bash) or change accordingly to your shell.
sudo echo "prefix=$HOME/software/ptouch-print" | tee -a ~/.zshenv ~/.bashrc
sudo echo "PATH=$prefix/bin:$PATH" | tee -a ~/.zshenv ~/.bashrc
sudo echo "MANPATH=$prefix/share/man/man1:$MANPATH" | tee -a ~/.zshenv ~/.bashrc

```

## How to use

ptouch print comes with 3 preconfigured template: 

## Test


``` 
bb --nrepl-server
```
 Then cider-connect to port.
