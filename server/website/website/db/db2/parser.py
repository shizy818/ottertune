import re

from ..base.parser import BaseParser
from website.utils import ConversionUtil


class Db2Parser(BaseParser):

    def __init__(self, dbms_obj):
        super().__init__(dbms_obj)
        self.valid_true_val = ("on", "true", "yes", 1)
        self.valid_false_val = ("off", "false", "no", 0)

    def parse_version_string(self, version_string):
        print("version_string", version_string)
        return version_string
        # dbms_version = version_string.split(',')[0]
        # return re.search(r'\d+\.\d+(?=\.\d+)', dbms_version).group(0)