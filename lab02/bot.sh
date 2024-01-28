#!/usr/bin/env bash
echo $1
sleep 3

for i in {0..10..1}; do
    echo "у меня появилось новое предложение"
    sleep 2
    if ! (( $i % 4 - 1 )); then
        echo "<<ТЕСЛА>>"
        sleep 1
        echo "уникальное предложение!"
        sleep 1
        echo "<<ТЕСЛА>>"
        sleep 1
        echo "можно взять вместе с гаражом"
        sleep 4
    elif ! (( $i % 4 )); then
        echo "продам гараж"
        sleep 4
    elif ! (( $i % 4 - 2 )); then
        echo "кому нибудь нужен мак?"
        sleep 3
    elif ! (( $i % 4 - 3 )); then
        echo "книга - лучший подарок на новый год"
        sleep 3
    fi
done
