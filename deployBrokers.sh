#!/bin/bash

# Carpeta local que se enviará a los servidores remotos
SOURCE_FOLDER="./iceGrid"

# Lista de direcciones remotas en el formato usuario@host:destino
REMOTE_ADDRESSES=(
    "computacion2@xhgrid10:/home/computacion2/broker/config"
    "computacion2@xhgrid19:/home/computacion2/broker/config"
)


# Ruta absoluta en el servidor remoto donde se almacenarán los archivos de configuración
CONFIG_FILE_ROUTE="/home/computacion2/broker/config/iceGrid"

# Lista de nombres de archivos de configuración que se registrarán
CONFIG_FILES_NAME=(
    "registryConsultantProxy.grid"
    "registryConsultantClient.grid"
)

# Lista de rutas de LMDB correspondientes a cada archivo de configuración
LMDB_PATHS=(
    "/home/computacion2/logs/registryConsultantProxy"
    "/home/computacion2/logs/registryConsultantClient"

)

# Contraseña para autenticación SSH (nota: almacenar contraseñas en scripts no es seguro)
PASSWORD="computacion2"

# Verificar que las listas REMOTE_ADDRESSES, CONFIG_FILES_NAME y LMDB_PATHS tengan la misma longitud
if [ ${#REMOTE_ADDRESSES[@]} -ne ${#CONFIG_FILES_NAME[@]} ] || [ ${#REMOTE_ADDRESSES[@]} -ne ${#LMDB_PATHS[@]} ]; then
    echo "Error: Las listas REMOTE_ADDRESSES, CONFIG_FILES_NAME y LMDB_PATHS deben tener la misma longitud."
    exit 1
fi

# Iterar sobre los índices de las listas

(
for i in "${!REMOTE_ADDRESSES[@]}"; do
    REMOTE="${REMOTE_ADDRESSES[$i]}"
    CONFIG_FILE="${CONFIG_FILES_NAME[$i]}"
    LMDB_PATH="${LMDB_PATHS[$i]}"

    (
    echo "----------------------------------------"
    echo "Iniciando despliegue en $REMOTE con $CONFIG_FILE"

    # Extraer el usuario y host de la dirección remota
    USER_HOST=$(echo "$REMOTE" | cut -d':' -f1)

    echo "Usuario y host: $USER_HOST  |"

    # Crear la ruta remota de configuración si no existe
    echo "Creando directorio remoto en $USER_HOST:/home/computacion2/broker/config"
    sshpass -p "$PASSWORD" ssh "$USER_HOST" "mkdir -p /home/computacion2/broker/config"

    # Verificar si la creación del directorio fue exitosa
    if [ $? -ne 0 ]; then
        echo "Error al crear el directorio remoto en $USER_HOST."
        echo "Despliegue en $REMOTE finalizado con errores."
        echo "----------------------------------------"
        exit 1
    fi

    # Crear el directorio de logs remoto si no existe
    echo "Creando directorio de logs en $USER_HOST:$LMDB_PATH"
    sshpass -p "$PASSWORD" ssh "$USER_HOST" "mkdir -p $LMDB_PATH"

    # Verificar si la creación del directorio de logs fue exitosa
    if [ $? -ne 0 ]; then
        echo "Error al crear el directorio de logs en $USER_HOST."
        echo "Despliegue en $REMOTE finalizado con errores."
        echo "----------------------------------------"
        exit 1
    fi

    # Copiar la carpeta iceGrid al destino remoto usando scp
    echo "Copiando $SOURCE_FOLDER a $REMOTE"
    sshpass -p "$PASSWORD" scp -r "$SOURCE_FOLDER" "$REMOTE"

    # Verificar si la copia fue exitosa
    if [ $? -eq 0 ]; then
        echo "Copia a $REMOTE completada con éxito."

        # Construir la ruta completa al archivo de configuración en el servidor remoto
        CONFIG_PATH="$CONFIG_FILE_ROUTE/$CONFIG_FILE"

        echo "Ejecutando icegridregistry con $CONFIG_PATH en $USER_HOST"

        # Ejecutar el comando icegridregistry en el servidor remoto

        sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER_HOST" " setsid /usr/bin/icegridregistry --Ice.Config=$CONFIG_PATH & exit"

        # Verificar si el comando se ejecutó correctamente
        if [ $? -eq 0 ]; then
            echo "Registro de $CONFIG_FILE en $USER_HOST completado con éxito."
        else
            echo "Error al registrar $CONFIG_FILE en $USER_HOST."
        fi

    else
        echo "Error al copiar a $REMOTE."
    fi

    echo "Despliegue en $REMOTE finalizado."
    echo "----------------------------------------"
    ) &  # Ejecutar cada despliegue en segundo plano
done

# Esperar a que todos los procesos en segundo plano terminen
wait
) &
echo "Despliegue de Brokers completado."
