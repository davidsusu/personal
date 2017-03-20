#!/bin/bash

x=300
y=300
r=100
angle=720
step=10

pi=$(echo "scale=10; 4*a(1)" | bc -l)

for arg in `seq 0 $step $angle`
do

rad=$(echo "scale=10; $pi * $arg / 180" | bc)
mx=$(echo "$x + (c($rad) * $r)" | bc -l)
my=$(echo "$y + (s($rad) * $r)" | bc -l)

sleep 0.01
xdotool mousemove $mx $my

done