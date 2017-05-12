import os

class TemporaryEnv():
    '''
    Set a enviroment variable temporary until the with clause ends
    '''
    def __init__(self, name, val):
        self.name = name
        self.val = val
        self.oldVal = None

    def __enter__(self):
        self.oldVal = os.environ.get(self.name)
        os.environ[self.name] = self.val
        return os.environ

    def __exit__(self, type, value, traceback):
        if self.oldVal is not None:
            os.environ[self.name] = self.oldVa
        elif self.name in os.environ:
            del os.environ[self.name]
