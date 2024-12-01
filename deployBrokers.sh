#!/bin/bash

SOURCE_FOLDER="./iceGrid"

REMOTE_ADDRESSES=(
    "computacion2@xhgrid10:~/broker/config"
    "computacion2@xhgrid11:~/broker/config"
)

PASSWORD="computacion2"

for REMOTE in "${REMOTE_ADDRESSES[@]}"
do
    echo "Enviando $SOURCE_FOLDER a $REMOTE"
    sshpass -p "$PASSWORD" scp -r "$SOURCE_FOLDER" "$REMOTE"
    if [ $? -eq 0 ]; then
        echo "Copia a $REMOTE completada con éxito."
    else
        echo "Error al copiar a $REMOTE."
    fi
done
