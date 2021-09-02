from ..base.target_objective import BaseThroughput
from website.types import DBMSType

target_objective_list = tuple((DBMSType.DB2, target_obj) for target_obj in [  # pylint: disable=invalid-name
    BaseThroughput(transactions_counter='sysibmadm.total_app_commits')
])