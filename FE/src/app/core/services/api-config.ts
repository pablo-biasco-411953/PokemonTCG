export function getBackendUrl(): string {
  const host = window.location.hostname;
  const port = window.location.port;
  
  // Si estamos en entorno local (tiene puerto de dev o coincide con nombres/IPs locales), usa el puerto 8080 local
  if (
    port ||
    host === 'localhost' ||
    host === '127.0.0.1' ||
    host.startsWith('192.168.') ||
    host.startsWith('10.') ||
    host.startsWith('172.16.') ||
    host.endsWith('.local')
  ) {
    return `http://${host}:8080`;
  }
  
  // URL DE PRODUCCIÓN: Reemplaza esta URL con la que te provea Koyeb o Render para tu backend
  // Por ejemplo: 'https://pokeutn-backend-tuusuario.koyeb.app'
  return 'https://pokemontcg-gi68.onrender.com';
}

export function getWsUrl(): string {
  const url = getBackendUrl();
  const wsProtocol = url.startsWith('https') ? 'wss' : 'ws';
  const rawHost = url.replace(/^https?:\/\//, '');
  return `${wsProtocol}://${rawHost}`;
}
