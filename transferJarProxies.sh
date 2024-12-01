#!/bin/bash

# -----------------------------
# Configuración del Script
# -----------------------------

# Verificar si se proporcionaron los argumentos necesarios
if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Uso: $0 /ruta/al/archivo.jar [/ruta/remota/destino]"
    exit 1
fi

# Ruta al archivo JAR a transferir
JAR_FILE="$1"

# Verificar si el archivo existe
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: El archivo $JAR_FILE no existe."
    exit 1
fi

# Ruta remota donde se copiará el archivo JAR (por defecto)
DEFAULT_REMOTE_PATH="~/icegrid_node/jars"

# Si se proporciona el segundo argumento, usarlo como ruta remota
if [ -n "$2" ]; then
    REMOTE_PATH="$2"
else
    REMOTE_PATH="$DEFAULT_REMOTE_PATH"
fi

# Mapeo de hosts (usuarios y direcciones IP o nombres de host)
HOSTS=(
    "computacion2@10.147.19.85"
    "computacion2@10.147.19.239"
    # Agrega más hosts según sea necesario
)

# Contraseña para SSH y SCP 
PASSWORD="computacion2"

# -----------------------------
# Transferencia del Archivo
# -----------------------------

for HOST in "${HOSTS[@]}"
do
    echo "Transfiriendo $JAR_FILE a $HOST:$REMOTE_PATH"

    # Crear el directorio remoto si no existe
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$HOST" "mkdir -p $REMOTE_PATH"

    # Transferir el archivo JAR
    sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no "$JAR_FILE" "$HOST:$REMOTE_PATH"

    if [ $? -eq 0 ]; then
        echo "Transferencia a $HOST completada."
    else
        echo "Error al transferir a $HOST."
    fi
done

echo "Transferencia a todos los hosts completada."
