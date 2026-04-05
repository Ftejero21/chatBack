ALTER TABLE mensajes
  ADD COLUMN IF NOT EXISTS contenido_busqueda TEXT NULL;
