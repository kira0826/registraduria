#!/bin/bash

# -----------------------------
# Configuración del Script
# -----------------------------

# Número de nodos a desplegar
NUM_NODES=6

# Lista de direcciones de los nodos (usuario@host)
NODE_ADDRESSES=(
    "computacion2@xhgrid17"
    "computacion2@xhgrid18"
    "computacion2@xhgrid5"
    "computacion2@xhgrid6"
    "computacion2@xhgrid7"
    "computacion2@xhgrid8"
)

# Lista de Endpoints para cada nodo (uno por nodo)
NODE_ENDPOINTS=(
    "tcp -h 10.147.19.85 -p 10001"
    "tcp -h 10.147.19.239 -p 10002"
    "tcp -h 10.147.19.112 -p 10003"
    "tcp -h 10.147.19.230 -p 10004"
    "tcp -h 10.147.19.139 -p 10005"
    "tcp -h 10.147.19.137 -p 10006"
)

# IP del Locator (Registro)
LOCATOR_IP="10.147.19.138"
LOCATOR_PORT="30000"

# Ruta al ejecutable de icegridnode
ICEGRIDNODE_EXEC="/home/computacion2/icegrid_node"  # Asegúrate de que esta es la ruta correcta

# Directorio donde se guardarán las configuraciones generadas
CONFIG_DIR="./iceGrid/nodes"

# Directorio remoto donde se copiarán las configuraciones y se ejecutará el nodo
REMOTE_DIR="/home/computacion2/icegrid_node"

# Contraseña para SSH y SCP (¡Cuidado con la seguridad!)
PASSWORD="computacion2"

# -----------------------------
# Comienzo del Script
# -----------------------------

# Verificar que el número de nodos coincide con las listas proporcionadas
if [ ${#NODE_ADDRESSES[@]} -ne $NUM_NODES ] || [ ${#NODE_ENDPOINTS[@]} -ne $NUM_NODES ]; then
    echo "Error: El número de nodos no coincide con las listas de direcciones y endpoints."
    exit 1
fi

# Crear el directorio de configuraciones si no existe
mkdir -p "$CONFIG_DIR"

(
    for (( i=0; i<$NUM_NODES; i++ ))
    do
        (
            NODE_NAME="Node$((i+1))"
            USER_AT_HOST="${NODE_ADDRESSES[$i]}"
            NODE_ENDPOINT="${NODE_ENDPOINTS[$i]}"

            CONFIG_FILE="$CONFIG_DIR/icegridnode_$NODE_NAME.conf"
            NODE_DATA_DIR="$REMOTE_DIR/logs/$NODE_NAME"

            echo "Generando configuración para $NODE_NAME"

            # Crear el archivo de configuración para el nodo
            cat > "$CONFIG_FILE" <<EOF
IceGrid.Node.Name=$NODE_NAME
IceGrid.Node.Endpoints=$NODE_ENDPOINT
IceGrid.Node.Data=$NODE_DATA_DIR
IceGrid.Node.CollocateRegistry=0

Ice.Default.Locator=registryConsultantClient/Locator:tcp -h $LOCATOR_IP -p $LOCATOR_PORT
EOF

            # Ruta local al archivo de configuración de admin
            LOCAL_ADMIN_CONF="./iceGrid/nodes/adminregistryclient.conf"

            # Verificar si el archivo existe
            if [ ! -f "$LOCAL_ADMIN_CONF" ]; then
                echo "Error: El archivo $LOCAL_ADMIN_CONF no existe."
                exit 1
            fi

            echo "Desplegando $NODE_NAME en $USER_AT_HOST"

            # Crear directorio remoto
            sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER_AT_HOST" "mkdir -p $NODE_DATA_DIR"

            # Copiar el archivo de configuración al nodo remoto
            sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no "$CONFIG_FILE" "$USER_AT_HOST:$REMOTE_DIR/icegridnode.conf"

            # Copiar el archivo adminregistryclient.conf al nodo remoto como admin.conf
            sshpass -p "$PASSWORD" scp -o StrictHostKeyChecking=no "$LOCAL_ADMIN_CONF" "$USER_AT_HOST:$REMOTE_DIR/admin.conf"

            echo "Iniciando $NODE_NAME en $USER_AT_HOST"

            # Iniciar el nodo en la máquina remota
            sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER_AT_HOST" "
                cd $REMOTE_DIR &&
                setsid /usr/bin/icegridnode --Ice.Config=icegridnode.conf > icegridnode.log 2>&1 < /dev/null &
                exit
            "

            echo "Ejecutando icegridadmin en segundo plano en $USER_AT_HOST"

            # Ejecutar icegridadmin en segundo plano
            sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER_AT_HOST" "
                cd $REMOTE_DIR &&
                setsid icegridadmin --Ice.Config=admin.conf -e \"application list\" > icegridadmin_output.log 2>&1 < /dev/null &
                exit
            "

            echo "$NODE_NAME desplegado y arrancado en $USER_AT_HOST"
        ) &
    done
    wait
    echo "Todos los nodos han sido desplegados y arrancados."
) &

echo "Despliegue completado."
