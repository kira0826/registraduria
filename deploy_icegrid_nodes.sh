#!/bin/bash

# -----------------------------
# Configuración del Script
# -----------------------------

# Número de nodos a desplegar
NUM_NODES=3

# Lista de direcciones de los nodos (usuario@host)
NODE_ADDRESSES=(
    "computacion2@direccion1"
    "usuario2@direccion2"
    "usuario3@direccion3"
)

# Lista de Endpoints para cada nodo (uno por nodo)
NODE_ENDPOINTS=(
    "tcp -h direccion1 -p 10001"
    "tcp -h direccion2 -p 10002"
    "tcp -h direccion3 -p 10003"
)

# IP y puerto del IceGrid Registry (Locator)
LOCATOR_IP="192.168.1.100"
LOCATOR_PORT="4061"

# Ruta local al ejecutable de icegridnode (ajusta según tu instalación)
ICEGRIDNODE_EXEC="/usr/bin/icegridnode"

# Directorio donde se guardarán las configuraciones generadas
CONFIG_DIR="./node_configs"

# Directorio remoto donde se copiarán las configuraciones y se ejecutará el nodo
REMOTE_DIR="~/icegrid_node"

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

# Generar configuraciones y desplegar nodos
for (( i=0; i<$NUM_NODES; i++ ))
do
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

Ice.Default.Locator=MyRegistry/Locator:tcp -h $LOCATOR_IP -p $LOCATOR_PORT
EOF

    # Desplegar el nodo en la máquina remota
    echo "Desplegando $NODE_NAME en $USER_AT_HOST"

    # Crear directorio remoto
    ssh "$USER_AT_HOST" "mkdir -p $NODE_DATA_DIR"

    # Copiar el archivo de configuración y el ejecutable (si es necesario) al nodo remoto
    scp "$CONFIG_FILE" "$USER_AT_HOST:$REMOTE_DIR/icegridnode.conf"

    # Iniciar el nodo en la máquina remota
    ssh "$USER_AT_HOST" "nohup $ICEGRIDNODE_EXEC --Ice.Config=$REMOTE_DIR/icegridnode.conf > $REMOTE_DIR/$NODE_NAME.log 2>&1 &"

    echo "$NODE_NAME desplegado y arrancado en $USER_AT_HOST"
done

echo "Despliegue completado."
