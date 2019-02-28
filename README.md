# Service Manager

##### Note: This is a maven project. In this project, I implemented two kinds of processing mode to handle concurrent invoke operation.

### ServiceManager Mode:

In this mode, manager can initialize the number of threads in thread pool. All invoke events will be operated
in these threads and the events will be in event queue maintained by ExecutorService. 

Max concurrent connections constraint will be handled using Semophor.

Pros: Main ExecutorService controls all events operations. Manager can choose thread number in thread pool.

Cons: Events will wait in the queue maintained by ExecutorService. It is not quite efficient. Because when an endpoint finishes the invoke operation, there will be an availble thread and next invoke event will get in. But if next invoke endpoint get its max concurrent connections, then the queue will be blocked. But actually, the second one or other endpoint invoke could be operated.

The bottlenect should be the minimum maxConcurrentConnections in all endpoins. 


### ServiceManagerDecentralizedThreadPool Mode:

In this mode, different endpoint maintains its own thread pool, each endpoint will have own event queue. 

Max concurrent connections constraint will be handled using Semophor.

Pros: Each endpoint maintains its own event queue. This is much more efficient. Because once an endpoint finishes the invoke operation, next invoking call will be polled from its queue.

Cons: The number of thread will be equal to the number endpoint at least. If there are lots of threads, it would be inefficient in some degree.
