def require_connection(fn):
  def wrapper(self, *args, **kwargs):
    if not self.conn:
      raise RuntimeError("You must call connect() first.")
    else:
      return fn(self, *args, **kwargs)
  return wrapper
