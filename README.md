## [사전 과제] 동시성 이슈 해결
[단기 스킬업 Redis 교육 과정](https://hh-skillup.oopy.io/) 시작 전, Thread Safe 한 코드를 통한 동시성 이슈 해결을 미리 경험하기 위한 과제입니다. 

## 문제 정의 
동시성 처리가 되지 않은 상태에서 공유 자원에 여러 스레드가 짧은 간격 혹은 동시에 접근할 때 발생하는 문제들이 있다. 크게 두 가지의 문제로 분류할 수 있는데 
한 가지는 메모리 가시성에 대한 문제이다. 동시에 여러 스레드가 접근할 때 공유자원이 변하는데 이때 스레드별로 OrderService 객체를 참조하고 있지만,
내부적으로 스레드들은 각자 캐시 메모리를 가지기 때문에 여기서 발생하는 문제가 있다. 캐시 메모리는 컨텍스트 스위칭과 같은 연산이 일어날 때 최신화 된다.
다른 한 가지 문제는 OrderService의 내부 코드 동작 문제이다. 메모리 문제가 해결되었다는 가정 하에서 order 메소드 실행시 내부 ```if(currentStock >= amount)``` 
해당 블럭에서 A스레드가 재고 감소 연산을 하기 직전에 B스레드가 if문 블럭을 통과하게 되면, B스레드가 막상 재고를 감소시키려고 할 때 재고가 부족할 수 있다. 
이 두 가지 문제를 이미지로 정리하면 아래와 같다. <br>
![스크린샷 2025-01-04 오전 11 57 19](https://github.com/user-attachments/assets/08bd3c56-0978-4242-b1af-ea0a6859ce5f)
![스크린샷 2025-01-04 오전 11 57 29](https://github.com/user-attachments/assets/3bf5da5b-bffe-4d90-974e-3ec0124f8691)

## 문제 해결
해결 방향은 메모리 가시성 문제와 시간차 코드 실행을 방지하는 것이다. 따라서 order() 메소드는 스레드가 동시에 실행할 수 없도록 락을 설정해주거나 CAS 연산을 사용하는 
concurrent 컬렉션 프레임워크를 사용해도 된다. 문제를 두 가지로 언급한 부분에 있어서 한가지 메모리 가시성 문제는 대부분의 동시성 처리를 해결하려는 노력들에서
해결이 되는 수준이다. CAS 연산도 cpu 레벨에서 메모리 변경을 관여하게 되고, ```synchronized``` 키워드를 사용하는 것도 메모리 가시성을 해결해준다. 내부적으로 ```synchronized``` 메소드
내부에서 사용한는 자원에 대해서는 캐시 메모리를 사용하지 않는다.

### concurrent 컬렉션 프레임워크 사용 
ConcurrentHashMap은 동시성 처리를 위한 컬렉션 프레임워크의 일부다. 
```java
productDatabase.compute(productName,(key, currentStock)->{ // 원자적 연산처리
  if (currentStock >= amount) {
    System.out.printf("%s 주문 정보: \n\t %s: 1건 ([%d])\n", Thread.currentThread().getName().substring(7), productName, amount);
    latestOrderDatabase.put(productName, new OrderInfo(productName, amount, System.currentTimeMillis()));
    return currentStock - amount;
  } else {
    return currentStock;
  }
});
```
기존의 코드를 위와 같이 변경했는데 이렇게 함으로써 order메소드 진입 직후의 재고를 기준으로 하여 재고를 업데이트 하게 된다. ConcurrentHashMap은 
compute() 메소드를 활용한 CAS연산을 지원하는데 CAS연산이란 원자적 연산을 위한 방식으로 CompareAndSwap방식으로 데이터를 업데이트하는 것으로 CPU가 직접 지원하는 연산이다. 
자바 코드에서 변경하려는 요소의 변경 전 데이터와 변경 후 데이터를 cpu에 전달하면 CPU에서는 데이터 변경 직전에 
현재 데이터가 전달받은 변경 전 데이터와 같은지 확인 후에 같다면 변경 후 데이터로 바꿔주는 방식이다. 만약 다르다면 실패로 처리하고
재시도를 하게 된다. 이렇게 함으로써 멀티 스레드 환경에서 스레드 세이프한 코드를 작성할 수 있다. 

### TO BE


## 문제 해결 
>>>>>>> Stashed changes

## 회고
이번 사전과제를 수행하면서 CAS연산과 synchronized 예약어 사용 중에 CAS연산을 적용하는 것이 더 좋다는 것을 알게 됐다. 다만, 머릿속으로 납득이 되지 않아서 약간의 테스트를 진행했다. 
```java
    long stTime = System.currentTimeMillis();
    long edTime = System.currentTimeMillis();
    System.out.println("ConcurrentHashMap 소요시간: " + (edTime - stTime) + "ms");
```
![스크린샷 2025-01-05 오전 10 01 56](https://github.com/user-attachments/assets/6deadc1f-d55f-4907-9172-11d086fd3226)
테스트는 같은 테스트 코드 내에서 시작지점과 끝 지점에 ```System.currentTimeMillis```코드를 사용해서 총 소요시간을 테스트한 것인데, ```concurrentHashMap```의 사용이 명확히 빨랐다.
이유는 아래와 같다. 느낌적으로 ```synchronized```는 단일 스레드의 처리를 완벽히 보장한다. 반면 ```concurrentHashMap```은 데이터의 정합성만을 중요시 여긴다.

```synchronized```의 경우 아래와 같은 과정이 필요하다.
1. 락의 점유 시도
2. BLOCKED 상태로 변경
3. 락의 점유 성공
4. RUNNABLE 상태로 변경
5. 코드 실행

 ```ConcurrentHashMap```은 아래와 같다.
1. 코드 실행
2. CAS연산 실패시 재실행

어떤 한 메소드를 실행하는데 필요한 과정이 차이가 있다. 그리고 표면적으로는 같은 작업인 것 같지만 ```ConcurrentHashMap```을 이용한 방식에서는 **모든 스레드가 동시에 코드를 동작시키고 있다** 
그리고 실패하면 재시도를 할 뿐이다. 반면 ```synchronized```는 실패할 일은 없지만 실패하지 않기 위한 많은 일련의 작업들이 필요한 것이다. 결론적으로 이러한 차이 때문에 ```Synchronized``` 와 같은
락 기반의 처리와 ```ConcurrentHashMap```과 같은 CAS를 활용하는 처리를 사용하는 경우가 따로 있으니 그때그때 잘 정해서 써야한다.

### 적용 시점의 차이
적용 시점의 차이를 나누자면 아래와 같긴 하지만 비즈니스 요구사항에 따라 중요도가 달라지기도 하기 때문에 그때그때 상황에 맞춰서 사용하는 것이 중요하다. 락 기반의 경우 실제 락 구현 방식을 변경하여 성능적 
이점을 취할 수도 있고, CAS연산도 상황에 맞춰서 코드를 잘 작성하면 단점을 어느정도 보완할 수 있을 것 같다. 

```락 기반 동시성 처리```
1. 단일 스레드 실행이 완벽히 보장돼야 하는 경우
2. 빈번한 BLOCKED는 성능 저하 유발 가능성 있음

```CAS연산 기반 동시성 처리```
1. 경합이 적고 고성능이 요구되는 경우
2. 무한 재시도 현상으로 인한 성능 저하 가능성이 있음

